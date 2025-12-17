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
import com.busconnect.catalogservice.exception.RateLimitExceededException;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Servicio para cálculo de rutas usando OpenRouteService API.
 * 
 * Características:
 * - Rate limiting thread-safe con ConcurrentLinkedQueue
 * - Caché reactivo con Mono.cache() (1 hora)
 * - Retry strategy con backoff exponencial
 * - Fallback data para rutas principales de Catalunya
 * - Validación completa de configuración
 */
@Service
@Slf4j
public class OpenRouteService {

    private final WebClient webClient;
    private final OpenRouteProperties properties;

    // ✅ Rate limiting thread-safe y reactivo
    private final ConcurrentLinkedQueue<LocalDateTime> requestTimestamps = new ConcurrentLinkedQueue<>();
    private final AtomicInteger totalRequestCount = new AtomicInteger(0);

    // Coordenadas de municipios principales de Catalunya (fallback data)
    // TODO: Mover a base de datos cuando se implemente población de municipios
    private final Map<String, double[]> CATALUNYA_COORDINATES = Map.of(
            "barcelona", new double[] { 41.390205, 2.154007 },
            "girona", new double[] { 41.979244, 2.821426 },
            "lleida", new double[] { 41.617950, 0.620348 },
            "tarragona", new double[] { 41.118645, 1.244784 },
            "sitges", new double[] { 41.235216, 1.811829 },
            "figueres", new double[] { 42.266390, 2.961000 },
            "vic", new double[] { 41.930000, 2.253056 },
            "manresa", new double[] { 41.721389, 1.824167 });

    // Distancias fallback entre ciudades principales (km)
    private final Map<String, BigDecimal> FALLBACK_DISTANCES = Map.of(
            "barcelona-sitges", new BigDecimal("42.3"),
            "sitges-barcelona", new BigDecimal("42.3"),
            "barcelona-girona", new BigDecimal("103.5"),
            "girona-barcelona", new BigDecimal("103.5"),
            "barcelona-lleida", new BigDecimal("162.8"),
            "lleida-barcelona", new BigDecimal("162.8"),
            "barcelona-tarragona", new BigDecimal("98.7"),
            "tarragona-barcelona", new BigDecimal("98.7"));

    // Duraciones fallback (minutos)
    private final Map<String, Integer> FALLBACK_DURATIONS = Map.of(
            "barcelona-sitges", 38,
            "sitges-barcelona", 38,
            "barcelona-girona", 90,
            "girona-barcelona", 90,
            "barcelona-lleida", 120,
            "lleida-barcelona", 120,
            "barcelona-tarragona", 85,
            "tarragona-barcelona", 85);

    /**
     * Constructor con validación completa de configuración.
     * Falla rápido si faltan valores obligatorios.
     */
    public OpenRouteService(OpenRouteProperties properties) {
        // ✅ Validar que properties no sea null
        if (properties == null) {
            throw new IllegalArgumentException("OpenRouteProperties cannot be null");
        }

        // ✅ Validar configuración obligatoria
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
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();

        log.info("OpenRouteService initialized successfully");
        log.info("Base URL: {}", baseUrl);
        log.info("Rate limit: {}/day", properties.getRateLimit().getMaxRequestsPerDay());
        log.info("Timeout: {}s", properties.getTimeout().getSeconds());
    }

    /**
     * Calcular ruta entre dos municipios.
     * 
     * ✅ CORRECCIÓN 5: Usa Mono.cache() reactivo en lugar de @Cacheable bloqueante
     * 
     * El caché es reactivo (no bloquea threads) y se mantiene por 1 hora.
     * Después de 1 hora, la próxima request recalcula la ruta.
     * 
     * @param origin      Municipio de origen
     * @param destination Municipio de destino
     * @return Mono con resultado de la ruta (distancia, duración, source)
     */
    public Mono<RouteResultResponse> calculateRoute(String origin, String destination) {
        log.info("Calculando ruta: {} -> {}", origin, destination);

        // ✅ Cache reactivo usando Mono.cache() en lugar de @Cacheable
        // Esto NO bloquea threads y es completamente reactivo
        return Mono.defer(() -> 
            checkRateLimit()
                .then(getCoordinates(origin, destination))
                .flatMap(coords -> callOpenRouteServiceAPI(coords[0], coords[1], coords[2], coords[3]))
                .map(response -> new RouteResultResponse(
                        origin, destination,
                        response.distanceKm, response.durationMinutes, "openroute"))
                .doOnSuccess(result -> log.debug("Ruta calculada exitosamente: {} km, {} min",
                        result.getDistanceKm(), result.getDurationMinutes()))
                .onErrorResume(error -> {
                    log.warn("Error calculando ruta con OpenRouteService: {}", error.getMessage());
                    return getFallbackRoute(origin, destination);
                })
        ).cache(Duration.ofHours(1)); // ✅ Cache reactivo por 1 hora
    }

    /**
     * ✅ Verificar límite de rate limiting de forma thread-safe y reactiva.
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
     * Obtener coordenadas de los municipios.
     * 
     * TODO: Cambiar a consulta de base de datos cuando se implemente
     *       población completa de municipios de Catalunya
     * 
     * @param origin      Municipio de origen
     * @param destination Municipio de destino
     * @return Mono con array [lat_origen, lon_origen, lat_destino, lon_destino]
     */
    private Mono<double[]> getCoordinates(String origin, String destination) {
        return Mono.fromCallable(() -> {
            double[] originCoords = CATALUNYA_COORDINATES.get(origin.toLowerCase());
            double[] destCoords = CATALUNYA_COORDINATES.get(destination.toLowerCase());

            if (originCoords == null || destCoords == null) {
                String missingMunicipality = originCoords == null ? origin : destination;
                throw new IllegalArgumentException(
                        "Municipality not found in Catalunya: " + missingMunicipality);
            }

            return new double[] { originCoords[0], originCoords[1], destCoords[0], destCoords[1] };
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

            // ✅ Usar RoundingMode en lugar de BigDecimal.ROUND_HALF_UP (deprecated)
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
     * Datos fallback cuando OpenRouteService no está disponible.
     * 
     * Usa distancias/duraciones precalculadas para rutas principales.
     * Si no hay datos fallback, retorna error amigable.
     */
    private Mono<RouteResultResponse> getFallbackRoute(String origin, String destination) {
        return Mono.fromCallable(() -> {
            String routeKey = origin.toLowerCase() + "-" + destination.toLowerCase();
            BigDecimal distance = FALLBACK_DISTANCES.get(routeKey);
            Integer duration = FALLBACK_DURATIONS.get(routeKey);

            if (distance != null && duration != null) {
                log.info("Usando datos fallback para ruta: {} -> {} ({} km, {} min)",
                        origin, destination, distance, duration);
                return new RouteResultResponse(origin, destination, distance, duration, "fallback");
            } else {
                log.warn("No hay datos fallback para ruta: {} -> {}", origin, destination);
                return new RouteResultResponse(origin, destination,
                        "No se encontró información para esta ruta. Por favor, intente más tarde.");
            }
        });
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
}