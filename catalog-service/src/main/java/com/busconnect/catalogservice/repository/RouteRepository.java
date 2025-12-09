package com.busconnect.catalogservice.repository;

import com.busconnect.catalogservice.model.Route;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface RouteRepository extends R2dbcRepository<Route, UUID> {

    /**
     * Buscar ruta específica por origen y destino
     */
    @Query("SELECT * FROM catalog.routes " +
           "WHERE origin_municipality_id = :originId " +
           "AND destination_municipality_id = :destinationId " +
           "AND is_active = true")
    Mono<Route> findByOriginAndDestination(@Param("originId") UUID originId, 
                                         @Param("destinationId") UUID destinationId);

    /**
     * Buscar rutas desde un municipio de origen
     */
    @Query("SELECT * FROM catalog.routes " +
           "WHERE origin_municipality_id = :originId " +
           "AND is_active = true " +
           "ORDER BY distance_km")
    Flux<Route> findByOrigin(@Param("originId") UUID originId);

    /**
     * Buscar rutas hacia un municipio de destino
     */
    @Query("SELECT * FROM catalog.routes " +
           "WHERE destination_municipality_id = :destinationId " +
           "AND is_active = true " +
           "ORDER BY distance_km")
    Flux<Route> findByDestination(@Param("destinationId") UUID destinationId);

    /**
     * Buscar rutas que necesitan actualización de OpenRouteService
     * (más de 30 días sin actualizar)
     */
    @Query("SELECT * FROM catalog.routes " +
           "WHERE (ors_last_updated IS NULL OR ors_last_updated < :cutoffDate) " +
           "AND is_active = true " +
           "ORDER BY ors_last_updated ASC NULLS FIRST " +
           "LIMIT :maxResults")
    Flux<Route> findRoutesNeedingUpdate(@Param("cutoffDate") LocalDateTime cutoffDate, 
                                       @Param("maxResults") Integer maxResults);

    /**
     * Verificar si existe una ruta (en cualquier dirección)
     */
    @Query("SELECT COUNT(*) > 0 FROM catalog.routes " +
           "WHERE ((origin_municipality_id = :municipalityA AND destination_municipality_id = :municipalityB) " +
           "OR (origin_municipality_id = :municipalityB AND destination_municipality_id = :municipalityA)) " +
           "AND is_active = true")
    Mono<Boolean> existsBetweenMunicipalities(@Param("municipalityA") UUID municipalityA, 
                                            @Param("municipalityB") UUID municipalityB);

    /**
     * Buscar rutas próximas a coordenadas específicas (para búsquedas geográficas)
     */
    @Query("SELECT r.* FROM catalog.routes r " +
           "JOIN catalog.municipalities m1 ON r.origin_municipality_id = m1.id " +
           "JOIN catalog.municipalities m2 ON r.destination_municipality_id = m2.id " +
           "WHERE (ST_DWithin(ST_MakePoint(m1.longitude, m1.latitude), ST_MakePoint(:lon, :lat), :radiusKm * 1000) " +
           "OR ST_DWithin(ST_MakePoint(m2.longitude, m2.latitude), ST_MakePoint(:lon, :lat), :radiusKm * 1000)) " +
           "AND r.is_active = true " +
           "ORDER BY r.distance_km")
    Flux<Route> findNearCoordinates(@Param("lat") Double latitude, 
                                  @Param("lon") Double longitude, 
                                  @Param("radiusKm") Double radiusKm);
}
