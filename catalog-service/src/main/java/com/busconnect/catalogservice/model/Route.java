package com.busconnect.catalogservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "routes", schema = "catalog")
public class Route {

    @Id
    private UUID id;

    @NotNull
    @Column("origin_municipality_id")
    private UUID originMunicipalityId;

    @NotNull
    @Column("destination_municipality_id") 
    private UUID destinationMunicipalityId;

    @Column("distance_km")
    private BigDecimal distanceKm;

    @Column("estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Column("route_code")
    private String routeCode;

    @NotNull
    @Column("is_active")
    private Boolean active = true;

    @Column("ors_last_updated")
    private LocalDateTime orsLastUpdated;

    @Column("ors_geometry")
    private String orsGeometry;  // Para guardar la geometría de OpenRouteService

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    // Constructor conveniente
    public Route(UUID originId, UUID destinationId, BigDecimal distanceKm, Integer durationMinutes) {
        this.originMunicipalityId = originId;
        this.destinationMunicipalityId = destinationId;
        this.distanceKm = distanceKm;
        this.estimatedDurationMinutes = durationMinutes;
        this.active = true;
        this.orsLastUpdated = LocalDateTime.now();
    }
}