package com.busconnect.catalogservice.repository;

import com.busconnect.catalogservice.model.VehicleType;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface VehicleTypeRepository extends R2dbcRepository<VehicleType, UUID> {

    /**
     * Buscar tipos de vehículo por categoría
     */
    @Query("SELECT * FROM catalog.vehicle_types " +
           "WHERE category = :category AND is_active = true " +
           "ORDER BY min_passengers")
    Flux<VehicleType> findByCategory(@Param("category") VehicleType.VehicleCategory category);

    /**
     * Buscar tipos de vehículo que puedan acomodar un número de pasajeros
     */
    @Query("SELECT * FROM catalog.vehicle_types " +
           "WHERE min_passengers <= :passengers " +
           "AND max_passengers >= :passengers " +
           "AND is_active = true " +
           "ORDER BY min_passengers")
    Flux<VehicleType> findByPassengerCapacity(@Param("passengers") Integer passengers);

    /**
     * Buscar tipos de vehículo por rango de capacidad
     */
    @Query("SELECT * FROM catalog.vehicle_types " +
           "WHERE max_passengers BETWEEN :minCapacity AND :maxCapacity " +
           "AND is_active = true " +
           "ORDER BY max_passengers")
    Flux<VehicleType> findByCapacityRange(@Param("minCapacity") Integer minCapacity, 
                                         @Param("maxCapacity") Integer maxCapacity);

    /**
     * Buscar tipos de vehículo por nombre
     */
    @Query("SELECT * FROM catalog.vehicle_types " +
           "WHERE LOWER(name) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "AND is_active = true " +
           "ORDER BY name")
    Flux<VehicleType> findByNameContaining(@Param("name") String name);

    /**
     * Buscar todos los tipos de vehículo activos
     */
    @Query("SELECT * FROM catalog.vehicle_types WHERE is_active = true ORDER BY category, min_passengers")
    Flux<VehicleType> findAllActive();

    /**
     * Buscar tipos de vehículo con amenidades específicas
     */
    @Query("SELECT * FROM catalog.vehicle_types " +
           "WHERE :amenity = ANY(amenities) AND is_active = true " +
           "ORDER BY category, name")
    Flux<VehicleType> findByAmenity(@Param("amenity") String amenity);

    /**
     * Verificar si existe un tipo de vehículo con el nombre dado
     */
    @Query("SELECT COUNT(*) > 0 FROM catalog.vehicle_types " +
           "WHERE LOWER(name) = LOWER(:name) AND is_active = true")
    Mono<Boolean> existsByNameIgnoreCase(@Param("name") String name);
}