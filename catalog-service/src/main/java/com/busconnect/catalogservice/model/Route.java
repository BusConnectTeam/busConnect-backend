package com.busconnect.catalogservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "routes", schema = "catalog")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull(message = "Origin municipality is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_municipality_id", nullable = false)
    private Municipality originMunicipality;

    @NotNull(message = "Destination municipality is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_municipality_id", nullable = false)
    private Municipality destinationMunicipality;

    @DecimalMin(value = "0.1", message = "Distance must be positive")
    @Column(name = "distance_km", precision = 6, scale = 2)
    private BigDecimal distanceKm; // Desde OpenRouteService

    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes; // Desde OpenRouteService

    @Column(name = "route_code", length = 20)
    private String routeCode; // Ej: "BCN-STG"

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Cache de OpenRouteService
    @Column(name = "ors_last_updated")
    private LocalDateTime orsLastUpdated;

    @Column(name = "ors_geometry", columnDefinition = "TEXT")
    private String orsGeometry; // GeoJSON opcional para mapas

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Método helper para generar route code automáticamente
    public void generateRouteCode() {
        if (originMunicipality != null && destinationMunicipality != null) {
            String originCode = originMunicipality.getNormalizedName()
                    .substring(0, Math.min(3, originMunicipality.getNormalizedName().length()))
                    .toUpperCase();
            String destCode = destinationMunicipality.getNormalizedName()
                    .substring(0, Math.min(3, destinationMunicipality.getNormalizedName().length()))
                    .toUpperCase();
            this.routeCode = originCode + "-" + destCode;
        }
    }
}