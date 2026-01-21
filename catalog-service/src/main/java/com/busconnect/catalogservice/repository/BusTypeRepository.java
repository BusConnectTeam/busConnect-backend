package com.busconnect.catalogservice.repository;

import com.busconnect.catalogservice.model.BusType;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Repositorio para operaciones de base de datos de tipos de autobuses.
 */
@Repository
public interface BusTypeRepository extends R2dbcRepository<BusType, UUID> {

    /**
     * Busca todos los tipos de bus activos ordenados por capacidad.
     */
    @Query("SELECT * FROM catalog.bus_types WHERE is_active = true ORDER BY capacity")
    Flux<BusType> findAllActive();

    /**
     * Busca tipos de bus por empresa.
     */
    @Query("SELECT * FROM catalog.bus_types WHERE company_id = :companyId AND is_active = true ORDER BY capacity")
    Flux<BusType> findByCompanyId(@Param("companyId") UUID companyId);

    /**
     * Busca tipos de bus con capacidad mínima.
     */
    @Query("SELECT * FROM catalog.bus_types WHERE capacity >= :minCapacity AND is_active = true ORDER BY capacity")
    Flux<BusType> findByMinCapacity(@Param("minCapacity") Integer minCapacity);

    /**
     * Busca tipos de bus por rango de capacidad.
     */
    @Query("SELECT * FROM catalog.bus_types WHERE capacity >= :minCapacity AND capacity <= :maxCapacity AND is_active = true ORDER BY capacity")
    Flux<BusType> findByCapacityRange(@Param("minCapacity") Integer minCapacity, @Param("maxCapacity") Integer maxCapacity);

    /**
     * Busca tipos de bus con WiFi.
     */
    @Query("SELECT * FROM catalog.bus_types WHERE has_wifi = true AND is_active = true ORDER BY capacity")
    Flux<BusType> findWithWifi();

    /**
     * Busca tipos de bus con acceso para silla de ruedas.
     */
    @Query("SELECT * FROM catalog.bus_types WHERE has_wheelchair_access = true AND is_active = true ORDER BY capacity")
    Flux<BusType> findWithWheelchairAccess();

    /**
     * Busca tipos de bus por tipo de asiento.
     */
    @Query("SELECT * FROM catalog.bus_types WHERE LOWER(seat_type) = LOWER(:seatType) AND is_active = true ORDER BY capacity")
    Flux<BusType> findBySeatType(@Param("seatType") String seatType);

    /**
     * Cuenta tipos de bus por empresa.
     */
    @Query("SELECT COUNT(*) FROM catalog.bus_types WHERE company_id = :companyId AND is_active = true")
    Mono<Long> countByCompanyId(@Param("companyId") UUID companyId);

    /**
     * Busca tipos de bus con múltiples filtros.
     */
    @Query("""
        SELECT * FROM catalog.bus_types
        WHERE is_active = true
        AND (:minCapacity IS NULL OR capacity >= :minCapacity)
        AND (:hasWifi IS NULL OR has_wifi = :hasWifi)
        AND (:hasToilet IS NULL OR has_toilet = :hasToilet)
        AND (:hasWheelchairAccess IS NULL OR has_wheelchair_access = :hasWheelchairAccess)
        ORDER BY capacity
        """)
    Flux<BusType> findByFilters(
            @Param("minCapacity") Integer minCapacity,
            @Param("hasWifi") Boolean hasWifi,
            @Param("hasToilet") Boolean hasToilet,
            @Param("hasWheelchairAccess") Boolean hasWheelchairAccess
    );
}
