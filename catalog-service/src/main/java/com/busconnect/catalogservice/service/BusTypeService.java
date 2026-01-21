package com.busconnect.catalogservice.service;

import com.busconnect.catalogservice.model.BusType;
import com.busconnect.catalogservice.repository.BusTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Servicio para la gestión de tipos de autobuses.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BusTypeService {

    private final BusTypeRepository busTypeRepository;

    /**
     * Obtiene todos los tipos de bus activos.
     */
    public Flux<BusType> getAllActive() {
        log.debug("Obteniendo todos los tipos de bus activos");
        return busTypeRepository.findAllActive()
                .doOnSubscribe(subscription -> log.info("Consultando tipos de bus activos"))
                .doOnComplete(() -> log.debug("Consulta de tipos de bus completada"));
    }

    /**
     * Busca un tipo de bus por ID.
     */
    public Mono<BusType> findById(UUID id) {
        log.debug("Buscando tipo de bus por ID: {}", id);
        return busTypeRepository.findById(id)
                .doOnSuccess(busType -> {
                    if (busType != null) {
                        log.debug("Tipo de bus encontrado: {}", busType.getName());
                    } else {
                        log.debug("Tipo de bus no encontrado con ID: {}", id);
                    }
                });
    }

    /**
     * Busca tipos de bus por empresa.
     */
    public Flux<BusType> findByCompanyId(UUID companyId) {
        log.debug("Buscando tipos de bus para empresa: {}", companyId);
        return busTypeRepository.findByCompanyId(companyId)
                .doOnComplete(() -> log.debug("Búsqueda por empresa completada"));
    }

    /**
     * Busca tipos de bus con capacidad mínima.
     */
    public Flux<BusType> findByMinCapacity(Integer minCapacity) {
        log.debug("Buscando tipos de bus con capacidad mínima: {}", minCapacity);
        return busTypeRepository.findByMinCapacity(minCapacity)
                .doOnComplete(() -> log.debug("Búsqueda por capacidad completada"));
    }

    /**
     * Busca tipos de bus por rango de capacidad.
     */
    public Flux<BusType> findByCapacityRange(Integer minCapacity, Integer maxCapacity) {
        log.debug("Buscando tipos de bus con capacidad entre {} y {}", minCapacity, maxCapacity);
        return busTypeRepository.findByCapacityRange(minCapacity, maxCapacity)
                .doOnComplete(() -> log.debug("Búsqueda por rango de capacidad completada"));
    }

    /**
     * Busca tipos de bus con WiFi.
     */
    public Flux<BusType> findWithWifi() {
        log.debug("Buscando tipos de bus con WiFi");
        return busTypeRepository.findWithWifi();
    }

    /**
     * Busca tipos de bus con acceso para silla de ruedas.
     */
    public Flux<BusType> findWithWheelchairAccess() {
        log.debug("Buscando tipos de bus con acceso para silla de ruedas");
        return busTypeRepository.findWithWheelchairAccess();
    }

    /**
     * Busca tipos de bus por tipo de asiento.
     */
    public Flux<BusType> findBySeatType(String seatType) {
        log.debug("Buscando tipos de bus con asiento tipo: {}", seatType);
        return busTypeRepository.findBySeatType(seatType);
    }

    /**
     * Busca tipos de bus con múltiples filtros.
     */
    public Flux<BusType> findByFilters(Integer minCapacity, Boolean hasWifi,
                                        Boolean hasToilet, Boolean hasWheelchairAccess) {
        log.debug("Buscando tipos de bus con filtros - capacidad: {}, wifi: {}, wc: {}, accesible: {}",
                minCapacity, hasWifi, hasToilet, hasWheelchairAccess);
        return busTypeRepository.findByFilters(minCapacity, hasWifi, hasToilet, hasWheelchairAccess)
                .doOnComplete(() -> log.debug("Búsqueda con filtros completada"));
    }

    /**
     * Guarda un tipo de bus.
     */
    public Mono<BusType> save(BusType busType) {
        log.info("Guardando tipo de bus: {}", busType.getName());
        return busTypeRepository.save(busType)
                .doOnSuccess(saved -> log.info("Tipo de bus guardado exitosamente: {} (ID: {})",
                        saved.getName(), saved.getId()))
                .doOnError(error -> log.error("Error guardando tipo de bus {}: {}",
                        busType.getName(), error.getMessage()));
    }

    /**
     * Desactiva un tipo de bus (soft delete).
     */
    public Mono<BusType> deactivate(UUID id) {
        log.info("Desactivando tipo de bus con ID: {}", id);
        return busTypeRepository.findById(id)
                .flatMap(busType -> {
                    busType.setActive(false);
                    return busTypeRepository.save(busType);
                })
                .doOnSuccess(busType -> {
                    if (busType != null) {
                        log.info("Tipo de bus desactivado: {}", busType.getName());
                    }
                });
    }

    /**
     * Cuenta tipos de bus por empresa.
     */
    public Mono<Long> countByCompanyId(UUID companyId) {
        return busTypeRepository.countByCompanyId(companyId)
                .doOnSuccess(count -> log.debug("Total tipos de bus para empresa {}: {}", companyId, count));
    }
}
