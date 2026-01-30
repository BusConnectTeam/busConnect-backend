package com.busconnect.catalogservice.repository;

import com.busconnect.catalogservice.model.BusCompany;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Repositorio para operaciones de base de datos de empresas de autobuses.
 */
@Repository
public interface BusCompanyRepository extends R2dbcRepository<BusCompany, UUID> {

    /**
     * Busca todas las empresas activas ordenadas por nombre.
     */
    @Query("SELECT * FROM catalog.bus_companies WHERE is_active = true ORDER BY name")
    Flux<BusCompany> findAllActive();

    /**
     * Busca una empresa por su CIF.
     */
    @Query("SELECT * FROM catalog.bus_companies WHERE cif = :cif AND is_active = true")
    Mono<BusCompany> findByCif(@Param("cif") String cif);

    /**
     * Busca empresas por nombre (búsqueda parcial, case insensitive).
     */
    @Query("SELECT * FROM catalog.bus_companies WHERE LOWER(name) LIKE LOWER(CONCAT('%', :name, '%')) AND is_active = true ORDER BY name")
    Flux<BusCompany> searchByName(@Param("name") String name);

    /**
     * Busca empresas por ciudad.
     */
    @Query("SELECT * FROM catalog.bus_companies WHERE LOWER(city) = LOWER(:city) AND is_active = true ORDER BY name")
    Flux<BusCompany> findByCity(@Param("city") String city);

    /**
     * Verifica si existe una empresa con el CIF dado.
     */
    @Query("SELECT COUNT(*) > 0 FROM catalog.bus_companies WHERE cif = :cif")
    Mono<Boolean> existsByCif(@Param("cif") String cif);

    /**
     * Cuenta el total de empresas activas.
     */
    @Query("SELECT COUNT(*) FROM catalog.bus_companies WHERE is_active = true")
    Mono<Long> countActive();
}
