package com.busconnect.catalogservice.controller;

import com.busconnect.catalogservice.dto.request.CalculateRouteRequest;
import com.busconnect.catalogservice.dto.response.RouteResultResponse;
import com.busconnect.catalogservice.model.Municipality;
import com.busconnect.catalogservice.service.OpenRouteService;
import com.busconnect.catalogservice.service.MunicipalityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Controlador REST para gestión de rutas y municipios de Catalunya.
 * 
 * Endpoints disponibles:
 * - POST /calculate: Cálculo de rutas (body JSON)
 * - GET /calculate: Cálculo de rutas (query params)
 * - GET /municipalities: Listar todos los municipios
 * - GET /municipalities/search: Buscar municipios por nombre
 * - GET /municipalities/{province}: Municipios por provincia
 * - GET /stream: Stream reactivo de múltiples rutas (LIMITADO A 20)
 * - GET /health: Health check del servicio
 * - GET /rate-limit-stats: Estadísticas de uso de API
 */
@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Routes", description = "API para cálculo de rutas entre municipios de Catalunya")
public class RouteController {

    private final OpenRouteService openRouteService;
    private final MunicipalityService municipalityService;

    /**
     * Calcular ruta entre dos municipios (POST).
     * Acepta JSON body con origen y destino.
     */
    @PostMapping("/calculate")
    @Operation(summary = "Calcular ruta entre municipios", 
               description = "Calcula distancia y duración entre dos municipios de Catalunya usando OpenRouteService")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ruta calculada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Municipios inválidos o datos incorrectos"),
        @ApiResponse(responseCode = "429", description = "Límite de requests excedido"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Mono<ResponseEntity<RouteResultResponse>> calculateRoute(
            @Valid @RequestBody CalculateRouteRequest request) {
        
        // ✅ CORRECCIÓN 7: Log a nivel INFO para requests (sin datos sensibles)
        log.info("Route calculation requested: {} -> {}", 
                request.getOriginMunicipality(), request.getDestinationMunicipality());
        
        return openRouteService.calculateRoute(
                request.getOriginMunicipality(), 
                request.getDestinationMunicipality())
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.debug("Route calculated successfully"))  // ✅ DEBUG, no INFO
                .doOnError(error -> log.error("Route calculation failed: {}", error.getMessage()));
    }

    /**
     * Calcular ruta entre dos municipios (GET).
     * Versión simplificada con query parameters.
     */
    @GetMapping("/calculate")
    @Operation(summary = "Calcular ruta entre municipios (GET)", 
               description = "Versión GET para cálculo rápido de rutas")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ruta calculada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Parámetros inválidos"),
        @ApiResponse(responseCode = "429", description = "Límite de requests excedido")
    })
    public Mono<ResponseEntity<RouteResultResponse>> calculateRouteGet(
            @Parameter(description = "Municipio de origen", example = "Barcelona")
            @RequestParam String origin,
            @Parameter(description = "Municipio de destino", example = "Sitges") 
            @RequestParam String destination) {
        
        log.info("GET - Route calculation: {} -> {}", origin, destination);
        
        return openRouteService.calculateRoute(origin, destination)
                .map(ResponseEntity::ok)
                .doOnError(error -> log.error("GET route calculation failed: {}", error.getMessage()));
    }

    /**
     * Obtener lista completa de municipios activos.
     */
    @GetMapping("/municipalities")
    @Operation(summary = "Obtener todos los municipios", 
               description = "Lista todos los municipios activos de Catalunya")
    @ApiResponse(responseCode = "200", description = "Lista de municipios obtenida exitosamente")
    public Flux<Municipality> getAllMunicipalities() {
        log.debug("Requesting all municipalities");  // ✅ DEBUG, no INFO
        return municipalityService.getAllActive()
                .doOnSubscribe(subscription -> log.info("Sending municipalities list"))
                .doOnComplete(() -> log.debug("Municipalities list sent"));
    }

    /**
     * Buscar municipios por nombre (búsqueda parcial).
     * Útil para autocompletado en frontend.
     */
    @GetMapping("/municipalities/search")
    @Operation(summary = "Buscar municipios", 
               description = "Busca municipios por nombre (parcial). Ideal para autocompletado.")
    @ApiResponse(responseCode = "200", description = "Municipios encontrados")
    public Flux<Municipality> searchMunicipalities(
            @Parameter(description = "Nombre o parte del nombre del municipio", example = "Barc")
            @RequestParam String name) {
        
        log.debug("Municipality search: '{}'", name);  // ✅ DEBUG, no INFO
        
        return municipalityService.searchByName(name)
                .doOnNext(municipality -> log.trace("Municipality found: {}", municipality.getName()))  // ✅ TRACE
                .doOnComplete(() -> log.debug("Search completed for: '{}'", name));
    }

    /**
     * Obtener municipios por provincia.
     */
    @GetMapping("/municipalities/{province}")
    @Operation(summary = "Municipios por provincia", 
               description = "Obtiene todos los municipios de una provincia específica")
    @ApiResponse(responseCode = "200", description = "Municipios de la provincia obtenidos")
    public Flux<Municipality> getMunicipalitiesByProvince(
            @Parameter(description = "Nombre de la provincia", example = "Barcelona")
            @PathVariable String province) {
        
        log.info("Municipalities requested for province: {}", province);
        
        return municipalityService.getByProvince(province)
                .doOnSubscribe(subscription -> log.debug("Searching municipalities in province: {}", province))
                .doOnComplete(() -> log.debug("Municipalities from {} sent", province));
    }

    /**
     * ✅ CORRECCIÓN 6: Stream de cálculos de rutas con LÍMITE de 20 rutas.
     * 
     * Stream reactivo para calcular múltiples rutas en tiempo real.
     * IMPORTANTE: Limitado a máximo 20 rutas para prevenir abuso de API.
     * 
     * Si se solicitan más de 20 rutas, solo se procesan las primeras 20.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
        summary = "Stream de cálculos de rutas", 
        description = "Endpoint reactivo para múltiples cálculos en tiempo real. LIMITADO A 20 RUTAS para prevenir abuso de API."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stream iniciado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Arrays de origen/destino inválidos")
    })
    public Flux<RouteResultResponse> streamRouteCalculations(
            @Parameter(description = "Array de municipios de origen (máx 20)", example = "Barcelona,Girona,Lleida")
            @RequestParam String[] origins,
            @Parameter(description = "Array de municipios de destino (máx 20)", example = "Sitges,Tarragona,Vic")
            @RequestParam String[] destinations) {
        
        // ✅ Limitar a máximo 20 rutas para evitar abuso de API
        final int MAX_ROUTES = 20;
        int routeCount = Math.min(origins.length, destinations.length);
        
        if (routeCount > MAX_ROUTES) {
            log.warn("Stream request exceeded limit: {} routes requested, limiting to {}", 
                    routeCount, MAX_ROUTES);
            routeCount = MAX_ROUTES;
        }
        
        final int finalRouteCount = routeCount;
        log.info("Starting route calculations stream for {} routes", finalRouteCount);
        
        return Flux.range(0, finalRouteCount)
                .flatMap(i -> openRouteService.calculateRoute(origins[i], destinations[i])
                    .doOnNext(result -> log.debug("Stream - Route calculated: {} -> {}", 
                        result.getOrigin(), result.getDestination())))
                .doOnComplete(() -> log.info("Route calculations stream completed"));
    }

    /**
     * Health check del servicio.
     */
    @GetMapping("/health")
    @Operation(summary = "Health check del servicio", 
               description = "Verifica el estado del servicio de rutas")
    @ApiResponse(responseCode = "200", description = "Servicio funcionando correctamente")
    public Mono<ResponseEntity<String>> healthCheck() {
        return Mono.just(ResponseEntity.ok("Catalog Service - Routes API is running"))
                .doOnSubscribe(subscription -> log.debug("Health check requested"));
    }

    /**
     * Estadísticas de rate limiting en tiempo real.
     * Muestra cuántas requests se han hecho en las últimas 24h.
     */
    @GetMapping("/rate-limit-stats")
    @Operation(
        summary = "Estadísticas de rate limiting", 
        description = "Muestra el uso actual de la cuota de OpenRouteService API (últimas 24h)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Estadísticas obtenidas exitosamente"),
        @ApiResponse(responseCode = "500", description = "Error obteniendo estadísticas")
    })
    public Mono<ResponseEntity<OpenRouteService.RateLimitStats>> getRateLimitStats() {
        log.debug("Requesting rate limit statistics");
        
        return openRouteService.getRateLimitStats()
                .doOnSuccess(stats -> {
                    if (stats != null) {
                        log.info("Rate Limit Stats - Used: {}/{} requests ({:.1f}% of daily limit), " +
                                "Remaining: {}, Total all-time: {}",
                            stats.requestsLast24h(),
                            stats.maxRequestsPerDay(),
                         String.format(Locale.US, "%.1f", stats.usagePercentage()),
                            stats.remainingRequests(),
                            stats.totalRequestsAllTime());
                    }
                })
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
                .doOnError(error -> 
                    log.error("Error retrieving rate limit statistics: {}", error.getMessage()));
    }
}