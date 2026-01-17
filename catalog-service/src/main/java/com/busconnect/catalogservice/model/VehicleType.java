package com.busconnect.catalogservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "vehicle_types", schema = "catalog")
public class VehicleType {

    @Id
    private UUID id;

    @NotBlank(message = "{vehicle.type.name.required}")
    @Column("name")
    private String name;

    @NotNull(message = "{vehicle.category.required}")
    @Column("category")
    private VehicleCategory category;

    @NotNull(message = "{vehicle.min.passengers.required}")
    @Min(value = 1, message = "{vehicle.min.passengers.min}")
    @Column("min_passengers")
    private Integer minPassengers;

    @NotNull(message = "{vehicle.max.passengers.required}")
    @Min(value = 1, message = "{vehicle.max.passengers.min}")
    @Column("max_passengers")
    private Integer maxPassengers;

    @Column("amenities")
    private String[] amenities; // Array de amenities en PostgreSQL

    @Column("description")
    private String description;

    @NotNull
    @Column("is_active")
    private Boolean active = true;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    // Enum para categorías
    public enum VehicleCategory {
        MICROBUS,      // 4-16 plazas
        AUTOCAR,       // 20-50 plazas  
        AUTOCAR_LUJO   // 30-55 plazas con servicios premium
    }

    // Constructor conveniente
    public VehicleType(String name, VehicleCategory category, Integer minPassengers, Integer maxPassengers) {
        this.name = name;
        this.category = category;
        this.minPassengers = minPassengers;
        this.maxPassengers = maxPassengers;
        this.active = true;
    }

    // Método helper para verificar capacidad
    public boolean canAccommodate(int passengers) {
        return passengers >= minPassengers && passengers <= maxPassengers && active;
    }
}