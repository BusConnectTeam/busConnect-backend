package com.busconnect.catalogservice.controller;

import com.busconnect.catalogservice.model.BusCompany;
import com.busconnect.catalogservice.model.BusType;
import com.busconnect.catalogservice.model.Driver;
import com.busconnect.catalogservice.service.BusCompanyService;
import com.busconnect.catalogservice.service.BusTypeService;
import com.busconnect.catalogservice.service.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Controlador REST para la gestión de empresas de autobuses, tipos de buses y conductores.
 */
@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Companies", description = "API para gestión de empresas de autobuses, flotas y conductores")
public class CompanyController {

    private final BusCompanyService busCompanyService;
    private final BusTypeService busTypeService;
    private final DriverService driverService;

    // ==================== EMPRESAS ====================

    @GetMapping
    @Operation(summary = "Obtener todas las empresas", description = "Retorna la lista de todas las empresas de autobuses activas")
    @ApiResponse(responseCode = "200", description = "Lista de empresas obtenida exitosamente")
    public Flux<BusCompany> getAllCompanies() {
        log.debug("GET /api/companies - Obteniendo todas las empresas");
        return busCompanyService.getAllActive()
                .doOnSubscribe(s -> log.info("Enviando lista de empresas"))
                .doOnComplete(() -> log.debug("Lista de empresas enviada"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener empresa por ID", description = "Retorna una empresa específica por su ID")
    @ApiResponse(responseCode = "200", description = "Empresa encontrada")
    @ApiResponse(responseCode = "404", description = "Empresa no encontrada")
    public Mono<ResponseEntity<BusCompany>> getCompanyById(
            @Parameter(description = "ID de la empresa") @PathVariable UUID id) {
        log.debug("GET /api/companies/{} - Buscando empresa", id);
        return busCompanyService.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar empresas por nombre", description = "Busca empresas que coincidan con el nombre dado")
    public Flux<BusCompany> searchCompanies(
            @Parameter(description = "Nombre o parte del nombre de la empresa") @RequestParam String name) {
        log.debug("GET /api/companies/search?name={}", name);
        return busCompanyService.searchByName(name);
    }

    @GetMapping("/city/{city}")
    @Operation(summary = "Obtener empresas por ciudad", description = "Retorna empresas ubicadas en una ciudad específica")
    public Flux<BusCompany> getCompaniesByCity(
            @Parameter(description = "Nombre de la ciudad") @PathVariable String city) {
        log.debug("GET /api/companies/city/{}", city);
        return busCompanyService.findByCity(city);
    }

    @GetMapping("/count")
    @Operation(summary = "Contar empresas activas", description = "Retorna el número total de empresas activas")
    public Mono<Long> countCompanies() {
        log.debug("GET /api/companies/count");
        return busCompanyService.countActive();
    }

    // ==================== TIPOS DE BUS ====================

    @GetMapping("/{companyId}/buses")
    @Operation(summary = "Obtener tipos de bus de una empresa", description = "Retorna todos los tipos de bus de una empresa específica")
    public Flux<BusType> getBusesByCompany(
            @Parameter(description = "ID de la empresa") @PathVariable UUID companyId) {
        log.debug("GET /api/companies/{}/buses", companyId);
        return busTypeService.findByCompanyId(companyId);
    }

    @GetMapping("/buses")
    @Operation(summary = "Obtener todos los tipos de bus", description = "Retorna todos los tipos de bus activos con filtros opcionales")
    public Flux<BusType> getAllBuses(
            @Parameter(description = "Capacidad mínima") @RequestParam(required = false) Integer minCapacity,
            @Parameter(description = "Requiere WiFi") @RequestParam(required = false) Boolean hasWifi,
            @Parameter(description = "Requiere WC") @RequestParam(required = false) Boolean hasToilet,
            @Parameter(description = "Requiere acceso silla de ruedas") @RequestParam(required = false) Boolean hasWheelchairAccess) {
        log.debug("GET /api/companies/buses - filtros: minCapacity={}, wifi={}, wc={}, accesible={}",
                minCapacity, hasWifi, hasToilet, hasWheelchairAccess);

        // Si hay filtros, usar el método con filtros
        if (minCapacity != null || hasWifi != null || hasToilet != null || hasWheelchairAccess != null) {
            return busTypeService.findByFilters(minCapacity, hasWifi, hasToilet, hasWheelchairAccess);
        }
        return busTypeService.getAllActive();
    }

    @GetMapping("/buses/{id}")
    @Operation(summary = "Obtener tipo de bus por ID", description = "Retorna un tipo de bus específico")
    @ApiResponse(responseCode = "200", description = "Tipo de bus encontrado")
    @ApiResponse(responseCode = "404", description = "Tipo de bus no encontrado")
    public Mono<ResponseEntity<BusType>> getBusById(
            @Parameter(description = "ID del tipo de bus") @PathVariable UUID id) {
        log.debug("GET /api/companies/buses/{}", id);
        return busTypeService.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/buses/seat-type/{seatType}")
    @Operation(summary = "Obtener buses por tipo de asiento", description = "Retorna buses filtrados por tipo de asiento (standard, premium, vip)")
    public Flux<BusType> getBusesBySeatType(
            @Parameter(description = "Tipo de asiento: standard, premium, vip") @PathVariable String seatType) {
        log.debug("GET /api/companies/buses/seat-type/{}", seatType);
        return busTypeService.findBySeatType(seatType);
    }

    // ==================== CONDUCTORES ====================

    @GetMapping("/{companyId}/drivers")
    @Operation(summary = "Obtener conductores de una empresa", description = "Retorna todos los conductores de una empresa específica")
    public Flux<Driver> getDriversByCompany(
            @Parameter(description = "ID de la empresa") @PathVariable UUID companyId) {
        log.debug("GET /api/companies/{}/drivers", companyId);
        return driverService.findByCompanyId(companyId);
    }

    @GetMapping("/drivers")
    @Operation(summary = "Obtener todos los conductores", description = "Retorna todos los conductores activos")
    public Flux<Driver> getAllDrivers() {
        log.debug("GET /api/companies/drivers");
        return driverService.getAllActive();
    }

    @GetMapping("/drivers/{id}")
    @Operation(summary = "Obtener conductor por ID", description = "Retorna un conductor específico")
    @ApiResponse(responseCode = "200", description = "Conductor encontrado")
    @ApiResponse(responseCode = "404", description = "Conductor no encontrado")
    public Mono<ResponseEntity<Driver>> getDriverById(
            @Parameter(description = "ID del conductor") @PathVariable UUID id) {
        log.debug("GET /api/companies/drivers/{}", id);
        return driverService.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/drivers/search")
    @Operation(summary = "Buscar conductores por nombre", description = "Busca conductores por nombre o apellido")
    public Flux<Driver> searchDrivers(
            @Parameter(description = "Nombre o apellido del conductor") @RequestParam String name) {
        log.debug("GET /api/companies/drivers/search?name={}", name);
        return driverService.searchByName(name);
    }

    @GetMapping("/drivers/license/{licenseType}")
    @Operation(summary = "Obtener conductores por tipo de licencia", description = "Retorna conductores con un tipo de licencia específico")
    public Flux<Driver> getDriversByLicenseType(
            @Parameter(description = "Tipo de licencia: D, D+E") @PathVariable String licenseType) {
        log.debug("GET /api/companies/drivers/license/{}", licenseType);
        return driverService.findByLicenseType(licenseType);
    }

    @GetMapping("/drivers/language/{language}")
    @Operation(summary = "Obtener conductores por idioma", description = "Retorna conductores que hablan un idioma específico")
    public Flux<Driver> getDriversByLanguage(
            @Parameter(description = "Código de idioma: es, ca, en, fr") @PathVariable String language) {
        log.debug("GET /api/companies/drivers/language/{}", language);
        return driverService.findByLanguage(language);
    }

    @GetMapping("/drivers/experience/{minYears}")
    @Operation(summary = "Obtener conductores por experiencia", description = "Retorna conductores con experiencia mínima")
    public Flux<Driver> getDriversByExperience(
            @Parameter(description = "Años mínimos de experiencia") @PathVariable Integer minYears) {
        log.debug("GET /api/companies/drivers/experience/{}", minYears);
        return driverService.findByMinExperience(minYears);
    }

    @GetMapping("/drivers/expiring-license")
    @Operation(summary = "Obtener conductores con licencia por expirar", description = "Retorna conductores cuya licencia expira en los próximos 90 días")
    public Flux<Driver> getDriversWithExpiringLicense() {
        log.debug("GET /api/companies/drivers/expiring-license");
        return driverService.findWithExpiringLicense();
    }

    // ==================== ESTADÍSTICAS ====================

    @GetMapping("/{companyId}/stats")
    @Operation(summary = "Obtener estadísticas de una empresa", description = "Retorna el conteo de buses y conductores de una empresa")
    public Mono<CompanyStats> getCompanyStats(
            @Parameter(description = "ID de la empresa") @PathVariable UUID companyId) {
        log.debug("GET /api/companies/{}/stats", companyId);
        return Mono.zip(
                busTypeService.countByCompanyId(companyId),
                driverService.countByCompanyId(companyId)
        ).map(tuple -> new CompanyStats(companyId, tuple.getT1(), tuple.getT2()));
    }

    /**
     * Record para estadísticas de empresa.
     */
    public record CompanyStats(UUID companyId, Long busCount, Long driverCount) {}
}
