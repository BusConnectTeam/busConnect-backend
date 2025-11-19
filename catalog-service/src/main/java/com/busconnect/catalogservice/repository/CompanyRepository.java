package com.busconnect.catalogservice.repository;

import com.busconnect.catalogservice.model.Company;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface CompanyRepository extends R2dbcRepository<Company, UUID> {

    /**
     * Buscar empresa por nombre exacto (case insensitive)
     */
    @Query("SELECT * FROM catalog.companies WHERE LOWER(name) = LOWER(:name) AND is_active = true")
    Mono<Company> findByNameIgnoreCase(@Param("name") String name);

    /**
     * Buscar empresas verificadas
     */
    @Query("SELECT * FROM catalog.companies WHERE verified = true AND is_active = true ORDER BY name")
    Flux<Company> findVerifiedCompanies();

    /**
     * Buscar empresas por rating mínimo
     */
    @Query("SELECT * FROM catalog.companies " +
           "WHERE rating >= :minRating AND is_active = true " +
           "ORDER BY rating DESC, name")
    Flux<Company> findByMinRating(@Param("minRating") BigDecimal minRating);

    /**
     * Buscar empresas similares por nombre (para sugerencias)
     */
    @Query("SELECT * FROM catalog.companies " +
           "WHERE LOWER(name) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "AND is_active = true " +
           "ORDER BY LENGTH(name) " +
           "LIMIT 5")
    Flux<Company> findSimilarByName(@Param("name") String name);

    /**
     * Buscar todas las empresas activas
     */
    @Query("SELECT * FROM catalog.companies WHERE is_active = true ORDER BY name")
    Flux<Company> findAllActive();

    /**
     * Buscar empresas por external company ID
     */
    @Query("SELECT * FROM catalog.companies " +
           "WHERE external_company_id = :externalId AND is_active = true")
    Mono<Company> findByExternalCompanyId(@Param("externalId") String externalId);

    /**
     * Verificar si existe una empresa con el nombre dado
     */
    @Query("SELECT COUNT(*) > 0 FROM catalog.companies " +
           "WHERE LOWER(name) = LOWER(:name) AND is_active = true")
    Mono<Boolean> existsByNameIgnoreCase(@Param("name") String name);
}