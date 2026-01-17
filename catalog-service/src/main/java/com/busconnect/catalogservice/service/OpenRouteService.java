package com.busconnect.catalogservice.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.busconnect.catalogservice.config.OpenRouteProperties;
import com.busconnect.catalogservice.dto.response.RouteResultResponse;
import com.busconnect.catalogservice.exception.MunicipalityNotFoundException;
import com.busconnect.catalogservice.exception.RateLimitExceededException;
import com.busconnect.catalogservice.model.Municipality;
import com.busconnect.catalogservice.repository.MunicipalityRepository;
import com.github.benmanes.caffeine.cache.Cache;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Servicio para cálculo de rutas usando OpenRouteService API.
 *
 * Características:
 * - Rate limiting thread-safe con ConcurrentLinkedQueue
 * - Caché compartido con Caffeine para rutas y municipios
 * - Coordenadas obtenidas de base de datos (947 municipios de Catalunya)
 * - Retry strategy con backoff exponencial
 * - Validación completa de configuración
 */
@Service
@Slf4j
public class OpenRouteService {

    private final WebClient webClient;
    private final OpenRouteProperties properties;
    private final Cache<String, RouteResultResponse> routeCache;
    private final Cache<String, Municipality> municipalityCache;
    private final MunicipalityRepository municipalityRepository;

    // Rate limiting thread-safe y reactivo
    private final ConcurrentLinkedQueue<LocalDateTime> requestTimestamps = new ConcurrentLinkedQueue<>();
    private final AtomicInteger totalRequestCount = new AtomicInteger(0);

    /**
     * Constructor con validación completa de configuración.
     * Falla rápido si faltan valores obligatorios.
     */
    public OpenRouteService(OpenRouteProperties properties,
                            Cache<String, RouteResultResponse> routeCache,
                            Cache<String, Municipality> municipalityCache,
                            MunicipalityRepository municipalityRepository) {
        // Validar que properties no sea null
        if (properties == null) {
            throw new IllegalArgumentException("OpenRouteProperties cannot be null");
        }

        // Validar configuración obligatoria
        String baseUrl = properties.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("OpenRouteService base URL cannot be null or empty");
        }

        String apiKey = properties.getKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("OpenRouteService API key cannot be null or empty");
        }

        // Asignar después de validar
        this.properties = properties;
        this.routeCache = routeCache;
        this.municipalityCache = municipalityCache;
        this.municipalityRepository = municipalityRepository;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();

        log.info("OpenRouteService initialized successfully");
        log.info("Base URL: {}", baseUrl);
        log.info("Rate limit: {}/day", properties.getRateLimit().getMaxRequestsPerDay());
        log.info("Timeout: {}s", properties.getTimeout().getSeconds());
        log.info("Cache enabled: shared Caffeine cache for routes and municipalities");
    }

    /**
     * Calcular ruta entre dos municipios.
     *
     * Usa caché compartido Caffeine entre todos los usuarios:
     * - Si la ruta ya está cacheada, se devuelve inmediatamente (sin llamar a API)
     * - Si no está cacheada, se llama a OpenRouteService API y se guarda en caché
     * - El caché expira según configuración (default: 1 hora)
     *
     * Esto ahorra dinero al reducir llamadas a la API externa.
     *
     * @param origin      Municipio de origen
     * @param destination Municipio de destino
     * @return Mono con resultado de la ruta (distancia, duración, source)
     */
    public Mono<RouteResultResponse> calculateRoute(String origin, String destination) {
        String cacheKey = buildCacheKey(origin, destination);

        // Verificar caché primero (sin bloquear)
        RouteResultResponse cached = routeCache.getIfPresent(cacheKey);
        if (cached != null) {
            log.debug("Cache HIT para ruta: {} -> {}", origin, destination);
            return Mono.just(cached);
        }

        log.info("Cache MISS - Calculando ruta: {} -> {}", origin, destination);

        // Llamar a API y guardar en caché
        return checkRateLimit()
                .then(getCoordinates(origin, destination))
                .flatMap(coords -> callOpenRouteServiceAPI(coords[0], coords[1], coords[2], coords[3]))
                .map(response -> new RouteResultResponse(
                        origin, destination,
                        response.distanceKm, response.durationMinutes, "openroute"))
                .doOnSuccess(result -> {
                    routeCache.put(cacheKey, result);
                    log.debug("Ruta cacheada: {} -> {} ({} km, {} min)",
                            origin, destination, result.getDistanceKm(), result.getDurationMinutes());
                })
                .onErrorResume(error -> {
                    log.warn("Error calculando ruta con OpenRouteService: {}", error.getMessage());
                    return Mono.just(new RouteResultResponse(origin, destination,
                            "Error calculando ruta: " + error.getMessage()));
                });
    }

    /**
     * Construir clave de caché normalizada.
     */
    private String buildCacheKey(String origin, String destination) {
        return origin.toLowerCase().trim() + "-" + destination.toLowerCase().trim();
    }

    /**
     * Verificar límite de rate limiting de forma thread-safe y reactiva.
     *
     * Implementación thread-safe usando ConcurrentLinkedQueue:
     * - Registra timestamp de cada request
     * - Limpia automáticamente requests > 24h
     * - Cuenta requests reales (no threads)
     */
    private Mono<Void> checkRateLimit() {
        return Mono.defer(() -> {
            // Limpiar timestamps antiguos (más de 24 horas)
            LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
            requestTimestamps.removeIf(timestamp -> timestamp.isBefore(cutoff));

            // Verificar límite actual
            int currentCount = requestTimestamps.size();
            int maxRequests = properties.getRateLimit().getMaxRequestsPerDay();

            if (currentCount >= maxRequests) {
                log.warn("Rate limit exceeded: {}/{} requests in last 24h", currentCount, maxRequests);
                return Mono.error(new RateLimitExceededException(
                        String.format("Daily API limit exceeded. Current: %d/%d requests in last 24 hours",
                                currentCount, maxRequests)));
            }

            // Registrar este request (timestamp, no thread name)
            requestTimestamps.offer(LocalDateTime.now());
            totalRequestCount.incrementAndGet();

            log.debug("Rate limit check passed: {}/{} requests in last 24h",
                    currentCount + 1, maxRequests);

            return Mono.empty();
        });
    }

    /**
     * Obtener estadísticas del rate limiting en tiempo real.
     *
     * @return Mono con estadísticas actuales (requests 24h, límite, restantes, total, uso%)
     */
    public Mono<RateLimitStats> getRateLimitStats() {
        return Mono.fromCallable(() -> {
            // Limpiar timestamps antiguos antes de contar
            LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
            requestTimestamps.removeIf(timestamp -> timestamp.isBefore(cutoff));

            int requestsLast24h = requestTimestamps.size();
            int maxPerDay = properties.getRateLimit().getMaxRequestsPerDay();
            int remaining = Math.max(0, maxPerDay - requestsLast24h);
            double usagePercent = (requestsLast24h * 100.0) / maxPerDay;

            return new RateLimitStats(
                    requestsLast24h,
                    maxPerDay,
                    remaining,
                    totalRequestCount.get(),
                    usagePercent);
        });
    }

    /**
     * Obtener coordenadas de los municipios desde la base de datos.
     *
     * Usa caché Caffeine para evitar consultas repetidas a la BD.
     * Los municipios se cachean por 24 horas (no cambian frecuentemente).
     *
     * @param origin      Municipio de origen
     * @param destination Municipio de destino
     * @return Mono con array [lat_origen, lon_origen, lat_destino, lon_destino]
     */
    private Mono<double[]> getCoordinates(String origin, String destination) {
        return Mono.zip(
                getMunicipalityWithCache(origin),
                getMunicipalityWithCache(destination)
        ).map(tuple -> {
            Municipality originMunicipality = tuple.getT1();
            Municipality destMunicipality = tuple.getT2();

            return new double[]{
                    originMunicipality.getLatitude().doubleValue(),
                    originMunicipality.getLongitude().doubleValue(),
                    destMunicipality.getLatitude().doubleValue(),
                    destMunicipality.getLongitude().doubleValue()
            };
        });
    }

    /**
     * Obtener municipio con caché.
     *
     * Primero busca en caché Caffeine, si no existe consulta BD y lo cachea.
     */
    private Mono<Municipality> getMunicipalityWithCache(String name) {
        String normalizedName = name.toLowerCase().trim();

        // Verificar caché primero
        Municipality cached = municipalityCache.getIfPresent(normalizedName);
        if (cached != null) {
            log.debug("Municipality cache HIT: {}", name);
            return Mono.just(cached);
        }

        log.debug("Municipality cache MISS: {}", name);

        // Consultar BD y cachear
        return municipalityRepository.findByNameIgnoreCase(name)
                .switchIfEmpty(Mono.error(new MunicipalityNotFoundException(
                        "Municipio no encontrado en Catalunya: " + name)))
                .doOnSuccess(municipality -> {
                    if (municipality != null) {
                        municipalityCache.put(normalizedName, municipality);
                        log.debug("Municipality cached: {} ({}, {})",
                                municipality.getName(),
                                municipality.getLatitude(),
                                municipality.getLongitude());
                    }
                });
    }

    /**
     * Llamada real a OpenRouteService API con retry strategy mejorada.
     *
     * Características:
     * - 3 reintentos con backoff exponencial (2s, 4s, 8s)
     * - Solo reintenta errores 5xx del servidor
     * - Timeout configurable (default 20s)
     * - Manejo específico de error 429 (rate limit)
     */
    private Mono<OpenRouteResponse> callOpenRouteServiceAPI(double originLat, double originLon,
                                                            double destLat, double destLon) {

        String url = String.format("/v2/directions/driving-car?api_key=%s&start=%f,%f&end=%f,%f",
                properties.getKey(), originLon, originLat, destLon, destLat);

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::parseOpenRouteResponse)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .maxBackoff(Duration.ofSeconds(10))
                        .filter(throwable -> {
                            // Solo reintentar en errores 5xx del servidor
                            if (throwable instanceof WebClientResponseException) {
                                WebClientResponseException ex = (WebClientResponseException) throwable;
                                return ex.getStatusCode().is5xxServerError();
                            }
                            return false;
                        })
                        .doBeforeRetry(retrySignal ->
                                log.warn("Retrying OpenRouteService call, attempt: {}",
                                        retrySignal.totalRetries() + 1)))
                .timeout(properties.getTimeout())
                .onErrorMap(WebClientResponseException.class, ex -> {
                    if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                        return new RateLimitExceededException(
                                "OpenRouteService rate limit exceeded (429)");
                    }
                    return new RuntimeException(
                            "OpenRouteService API error: " + ex.getStatusCode() + " - " + ex.getMessage());
                });
    }

    /**
     * Parsear respuesta de OpenRouteService API.
     *
     * Extrae distancia (km) y duración (minutos) de la respuesta JSON.
     * Usa RoundingMode moderno (no deprecated ROUND_HALF_UP).
     */
    private OpenRouteResponse parseOpenRouteResponse(Map<String, Object> response) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> features = (Map<String, Object>)
                    ((java.util.List<?>) response.get("features")).get(0);

            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) features.get("properties");

            @SuppressWarnings("unchecked")
            Map<String, Object> segments = (Map<String, Object>)
                    ((java.util.List<?>) properties.get("segments")).get(0);

            double distanceMeters = ((Number) segments.get("distance")).doubleValue();
            double durationSeconds = ((Number) segments.get("duration")).doubleValue();

            // Convertir metros a kilómetros y segundos a minutos
            double distanceKm = distanceMeters / 1000.0;
            double durationMinutes = durationSeconds / 60.0;

            // Usar RoundingMode en lugar de BigDecimal.ROUND_HALF_UP (deprecated)
            BigDecimal distance = new BigDecimal(distanceKm)
                    .setScale(2, RoundingMode.HALF_UP);
            int duration = (int) Math.round(durationMinutes);

            log.debug("Parsed route: {} km, {} min", distance, duration);

            return new OpenRouteResponse(distance, duration);

        } catch (Exception e) {
            log.error("Error parsing OpenRouteService response", e);
            throw new RuntimeException("Error parsing OpenRouteService response: " + e.getMessage(), e);
        }
    }

    /**
     * Clase interna para respuesta de OpenRouteService API.
     */
    private static class OpenRouteResponse {
        final BigDecimal distanceKm;
        final Integer durationMinutes;

        OpenRouteResponse(BigDecimal distanceKm, Integer durationMinutes) {
            this.distanceKm = distanceKm;
            this.durationMinutes = durationMinutes;
        }
    }

    /**
     * Obtener estadísticas del caché de rutas.
     *
     * @return Mono con estadísticas del caché (hits, misses, tamaño, etc.)
     */
    public Mono<CacheStats> getCacheStats() {
        return Mono.fromCallable(() -> {
            var routeStats = routeCache.stats();
            var municipalityStats = municipalityCache.stats();

            return new CacheStats(
                    routeCache.estimatedSize(),
                    routeStats.hitCount(),
                    routeStats.missCount(),
                    routeStats.hitRate() * 100,
                    routeStats.evictionCount(),
                    municipalityCache.estimatedSize(),
                    municipalityStats.hitCount(),
                    municipalityStats.missCount());
        });
    }

    /**
     * Invalidar caché de una ruta específica.
     */
    public void invalidateRoute(String origin, String destination) {
        String cacheKey = buildCacheKey(origin, destination);
        routeCache.invalidate(cacheKey);
        log.info("Cache invalidado para ruta: {} -> {}", origin, destination);
    }

    /**
     * Invalidar todo el caché de rutas.
     */
    public void invalidateAllRoutes() {
        routeCache.invalidateAll();
        log.info("Todo el caché de rutas ha sido invalidado");
    }

    /**
     * Invalidar todo el caché de municipios.
     */
    public void invalidateAllMunicipalities() {
        municipalityCache.invalidateAll();
        log.info("Todo el caché de municipios ha sido invalidado");
    }

    /**
     * Record para estadísticas de rate limiting.
     * Usado por el endpoint /api/routes/rate-limit-stats
     */
    public record RateLimitStats(
            int requestsLast24h,
            int maxRequestsPerDay,
            int remainingRequests,
            int totalRequestsAllTime,
            double usagePercentage) {
    }

    /**
     * Record para estadísticas de caché (rutas + municipios).
     */
    public record CacheStats(
            long routeCacheSize,
            long routeHitCount,
            long routeMissCount,
            double routeHitRatePercent,
            long routeEvictionCount,
            long municipalityCacheSize,
            long municipalityHitCount,
            long municipalityMissCount) {
    }
}
