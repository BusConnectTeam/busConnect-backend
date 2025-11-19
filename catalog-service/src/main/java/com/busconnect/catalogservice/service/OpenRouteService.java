package com.busconnect.catalogservice.service;

import com.busconnect.catalogservice.config.OpenRouteProperties;
import com.busconnect.catalogservice.dto.response.RouteResultResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class OpenRouteService {

    private final WebClient webClient;
    private final OpenRouteProperties properties;
    private final Map<String, LocalDateTime> requestLog = new ConcurrentHashMap<>();
    
    // Coordenadas de municipios principales de Catalunya (fallback data)
    private final Map<String, double[]> CATALUNYA_COORDINATES = Map.of(
        "barcelona", new double[]{41.390205, 2.154007},
        "girona", new double[]{41.979244, 2.821426},
        "lleida", new double[]{41.617950, 0.620348},
        "tarragona", new double[]{41.118645, 1.244784},
        "sitges", new double[]{41.235216, 1.811829},
        "figueres", new double[]{42.266390, 2.961000},
        "vic", new double[]{41.930000, 2.253056},
        "manresa", new double[]{41.721389, 1.824167}
    );

    // Distancias fallback entre ciudades principales (km)
    private final Map<String, BigDecimal> FALLBACK_DISTANCES = Map.of(
        "barcelona-sitges", new BigDecimal("42.3"),
        "sitges-barcelona", new BigDecimal("42.3"),
        "barcelona-girona", new BigDecimal("103.5"),
        "girona-barcelona", new BigDecimal("103.5"),
        "barcelona-lleida", new BigDecimal("162.8"),
        "lleida-barcelona", new BigDecimal("162.8"),
        "barcelona-tarragona", new BigDecimal("98.7"),
        "tarragona-barcelona", new BigDecimal("98.7")
    );

    // Duraciones fallback (minutos)
    private final Map<String, Integer> FALLBACK_DURATIONS = Map.of(
        "barcelona-sitges", 38,
        "sitges-barcelona", 38,
        "barcelona-girona", 90,
        "girona-barcelona", 90,
        "barcelona-lleida", 120,
        "lleida-barcelona", 120,
        "barcelona-tarragona", 85,
        "tarragona-barcelona", 85
    );

    public OpenRouteService(OpenRouteProperties properties) {
        this.properties = properties;
        this.webClient = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    /**
     * Calcular ruta entre dos municipios (con cache)
     */
    @Cacheable(value = "routes", key = "#origin + '-' + #destination")
    public Mono<RouteResultResponse> calculateRoute(String origin, String destination) {
        log.info("Calculando ruta: {} -> {}", origin, destination);
        
        return checkRateLimit()
                .then(getCoordinates(origin, destination))
                .flatMap(coords -> callOpenRouteServiceAPI(coords[0], coords[1], coords[2], coords[3]))
                .map(response -> new RouteResultResponse(
                    origin, destination, 
                    response.distanceKm, response.durationMinutes, "openroute"))
                .doOnSuccess(result -> log.info("Ruta calculada exitosamente: {} km, {} min", 
                    result.getDistanceKm(), result.getDurationMinutes()))
                .onErrorResume(error -> {
                    log.warn("Error calculando ruta con OpenRouteService: {}", error.getMessage());
                    return getFallbackRoute(origin, destination);
                });
    }

    /**
     * Verificar límite de rate limiting
     */
    private Mono<Void> checkRateLimit() {
        return Mono.fromCallable(() -> {
            long todayRequests = requestLog.values().stream()
                .filter(timestamp -> timestamp.isAfter(LocalDateTime.now().minusDays(1)))
                .count();
            
            if (todayRequests >= properties.getRateLimit().getMaxRequestsPerDay()) {
                throw new RuntimeException("Daily API limit exceeded");
            }
            
            requestLog.put(Thread.currentThread().getName(), LocalDateTime.now());
            return null;
        });
    }

    /**
     * Obtener coordenadas de los municipios
     */
    private Mono<double[]> getCoordinates(String origin, String destination) {
        return Mono.fromCallable(() -> {
            double[] originCoords = CATALUNYA_COORDINATES.get(origin.toLowerCase());
            double[] destCoords = CATALUNYA_COORDINATES.get(destination.toLowerCase());
            
            if (originCoords == null || destCoords == null) {
                throw new RuntimeException("Municipality not found in Catalunya: " + 
                    (originCoords == null ? origin : destination));
            }
            
            return new double[]{originCoords[0], originCoords[1], destCoords[0], destCoords[1]};
        });
    }

    /**
     * Llamada real a OpenRouteService API
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
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)))
                .timeout(properties.getTimeout())
                .onErrorMap(WebClientResponseException.class, ex -> {
                    if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                        return new RuntimeException("Rate limit exceeded");
                    }
                    return new RuntimeException("OpenRouteService API error: " + ex.getMessage());
                });
    }

    /**
     * Parsear respuesta de OpenRouteService
     */
    private OpenRouteResponse parseOpenRouteResponse(Map<String, Object> response) {
        try {
            Map<String, Object> features = (Map<String, Object>) ((java.util.List<?>) response.get("features")).get(0);
            Map<String, Object> properties = (Map<String, Object>) features.get("properties");
            Map<String, Object> segments = (Map<String, Object>) ((java.util.List<?>) properties.get("segments")).get(0);
            
            double distance = ((Number) segments.get("distance")).doubleValue() / 1000.0;
            double duration = ((Number) segments.get("duration")).doubleValue() / 60.0;
            
            return new OpenRouteResponse(
                new BigDecimal(distance).setScale(2, BigDecimal.ROUND_HALF_UP),
                (int) Math.round(duration)
            );
        } catch (Exception e) {
            throw new RuntimeException("Error parsing OpenRouteService response: " + e.getMessage());
        }
    }

    /**
     * Datos fallback cuando OpenRouteService no está disponible
     */
    private Mono<RouteResultResponse> getFallbackRoute(String origin, String destination) {
        return Mono.fromCallable(() -> {
            String routeKey = origin.toLowerCase() + "-" + destination.toLowerCase();
            BigDecimal distance = FALLBACK_DISTANCES.get(routeKey);
            Integer duration = FALLBACK_DURATIONS.get(routeKey);
            
            if (distance != null && duration != null) {
                log.info("Usando datos fallback para ruta: {} -> {}", origin, destination);
                return new RouteResultResponse(origin, destination, distance, duration, "fallback");
            } else {
                return new RouteResultResponse(origin, destination, 
                    "No se encontró información para esta ruta");
            }
        });
    }

    private static class OpenRouteResponse {
        final BigDecimal distanceKm;
        final Integer durationMinutes;

        OpenRouteResponse(BigDecimal distanceKm, Integer durationMinutes) {
            this.distanceKm = distanceKm;
            this.durationMinutes = durationMinutes;
        }
    }
}