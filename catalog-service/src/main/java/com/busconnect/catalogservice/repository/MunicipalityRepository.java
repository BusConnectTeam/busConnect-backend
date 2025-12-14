package com.busconnect.catalogservice.repository;

import com.busconnect.catalogservice.model.Municipality;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface MunicipalityRepository extends R2dbcRepository<Municipality, UUID> {

    /**
     * Buscar municipio por nombre exacto (case insensitive)
     */
    @Query("SELECT * FROM catalog.municipalities WHERE LOWER(name) = LOWER(:name) AND is_active = true")
    Mono<Municipality> findByNameIgnoreCase(@Param("name") String name);

    /**
     * Buscar municipio por nombre normalizado
     */
    @Query("SELECT * FROM catalog.municipalities WHERE normalized_name = LOWER(:normalizedName) AND is_active = true")
    Mono<Municipality> findByNormalizedName(@Param("normalizedName") String normalizedName);

    /**
     * Buscar municipios por provincia
     */
    @Query("SELECT * FROM catalog.municipalities WHERE LOWER(province) = LOWER(:province) AND is_active = true")
    Flux<Municipality> findByProvinceIgnoreCase(@Param("province") String province);

    /**
     * Buscar municipios similares por nombre (para sugerencias)
     */
    @Query("SELECT * FROM catalog.municipalities " +
           "WHERE LOWER(name) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "AND is_active = true " +
           "ORDER BY LENGTH(name) " +
           "LIMIT 5")
    Flux<Municipality> findSimilarByName(@Param("name") String name);

    /**
     * Buscar todos los municipios activos de Catalunya
     */
    @Query("SELECT * FROM catalog.municipalities WHERE is_active = true ORDER BY name")
    Flux<Municipality> findAllActive();

    /**
     * Verificar si un municipio existe por coordenadas (aproximadas)
     */
    @Query("SELECT COUNT(*) > 0 FROM catalog.municipalities " +
           "WHERE ABS(latitude - :lat) < 0.01 " +
           "AND ABS(longitude - :lon) < 0.01 " +
           "AND is_active = true")
    Mono<Boolean> existsByCoordinates(@Param("lat") Double latitude, @Param("lon") Double longitude);
}