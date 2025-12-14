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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Routes", description = "API para cálculo de rutas entre municipios de Catalunya")
public class RouteController {

    private final OpenRouteService openRouteService;
    private final MunicipalityService municipalityService;

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
        
        log.info("Solicitud de cálculo de ruta: {} -> {}", 
                request.getOriginMunicipality(), request.getDestinationMunicipality());
        
        return openRouteService.calculateRoute(
                request.getOriginMunicipality(), 
                request.getDestinationMunicipality())
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.info("Ruta calculada exitosamente"))
                .doOnError(error -> log.error("Error calculando ruta: {}", error.getMessage()));
    }

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
        
        log.info("GET - Cálculo de ruta: {} -> {}", origin, destination);
        
        return openRouteService.calculateRoute(origin, destination)
                .map(ResponseEntity::ok)
                .doOnError(error -> log.error("Error en GET calculando ruta: {}", error.getMessage()));
    }

    @GetMapping("/municipalities")
    @Operation(summary = "Obtener todos los municipios", 
               description = "Lista todos los municipios activos de Catalunya")
    @ApiResponse(responseCode = "200", description = "Lista de municipios obtenida exitosamente")
    public Flux<Municipality> getAllMunicipalities() {
        log.debug("Solicitando lista de todos los municipios");
        return municipalityService.getAllActive()
                .doOnSubscribe(subscription -> log.info("Enviando lista de municipios"))
                .doOnComplete(() -> log.debug("Lista de municipios enviada"));
    }

    @GetMapping("/municipalities/search")
    @Operation(summary = "Buscar municipios", 
               description = "Busca municipios por nombre (parcial)")
    @ApiResponse(responseCode = "200", description = "Municipios encontrados")
    public Flux<Municipality> searchMunicipalities(
            @Parameter(description = "Nombre o parte del nombre del municipio", example = "Barc")
            @RequestParam String name) {
        
        log.info("Búsqueda de municipios: '{}'", name);
        
        return municipalityService.searchByName(name)
                .doOnNext(municipality -> log.debug("Municipio encontrado: {}", municipality.getName()))
                .doOnComplete(() -> log.info("Búsqueda completada para: '{}'", name));
    }

    @GetMapping("/municipalities/{province}")
    @Operation(summary = "Municipios por provincia", 
               description = "Obtiene todos los municipios de una provincia específica")
    @ApiResponse(responseCode = "200", description = "Municipios de la provincia obtenidos")
    public Flux<Municipality> getMunicipalitiesByProvince(
            @Parameter(description = "Nombre de la provincia", example = "Barcelona")
            @PathVariable String province) {
        
        log.info("Municipios solicitados para provincia: {}", province);
        
        return municipalityService.getByProvince(province)
                .doOnSubscribe(subscription -> log.info("Buscando municipios en provincia: {}", province))
                .doOnComplete(() -> log.debug("Municipios de {} enviados", province));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream de cálculos de rutas", 
               description = "Endpoint reactivo para múltiples cálculos en tiempo real")
    public Flux<RouteResultResponse> streamRouteCalculations(
            @RequestParam String[] origins,
            @RequestParam String[] destinations) {
        
        log.info("Iniciando stream de cálculos para {} rutas", origins.length);
        
        return Flux.range(0, Math.min(origins.length, destinations.length))
                .flatMap(i -> openRouteService.calculateRoute(origins[i], destinations[i])
                    .doOnNext(result -> log.debug("Stream - Ruta calculada: {} -> {}", 
                        result.getOrigin(), result.getDestination())))
                .doOnComplete(() -> log.info("Stream de cálculos completado"));
    }

    @GetMapping("/health")
    @Operation(summary = "Health check del servicio", 
               description = "Verifica el estado del servicio de rutas")
    @ApiResponse(responseCode = "200", description = "Servicio funcionando correctamente")
    public Mono<ResponseEntity<String>> healthCheck() {
        return Mono.just(ResponseEntity.ok("Catalog Service - Routes API is running"))
                .doOnSubscribe(subscription -> log.debug("Health check solicitado"));
    }
}
