package com.busconnect.catalogservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "vehicle_types", schema = "catalog")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "Vehicle type name is required")
    @Column(nullable = false, length = 100)
    private String name;

    @NotNull(message = "Category is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private VehicleCategory category;

    @NotNull(message = "Minimum passengers is required")
    @Min(value = 1, message = "Minimum passengers must be at least 1")
    @Column(name = "min_passengers", nullable = false)
    private Integer minPassengers;

    @NotNull(message = "Maximum passengers is required")
    @Min(value = 1, message = "Maximum passengers must be at least 1")
    @Column(name = "max_passengers", nullable = false)
    private Integer maxPassengers;

    @Column(columnDefinition = "text[]")
    private String[] amenities; // Array de amenities en PostgreSQL

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Enum para categorías, he pedido investigacion profunda para definirlas mejor
    public enum VehicleCategory {
        MICROBUS,      // 4-16 plazas
        AUTOCAR,       // 20-50 plazas  
        AUTOCAR_LUJO   // 30-55 plazas con servicios premium
    }
}
