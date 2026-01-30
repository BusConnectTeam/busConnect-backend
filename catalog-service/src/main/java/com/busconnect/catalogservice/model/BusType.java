package com.busconnect.catalogservice.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad que representa un tipo de autobús.
 * Incluye capacidad, amenidades y precio por kilómetro.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "bus_types", schema = "catalog")
public class BusType {

    @Id
    private UUID id;

    @NotNull(message = "El ID de la empresa es obligatorio")
    @Column("company_id")
    private UUID companyId;

    @NotBlank(message = "El nombre del tipo de bus es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    @Column("name")
    private String name;

    @NotNull(message = "La capacidad es obligatoria")
    @Min(value = 1, message = "La capacidad mínima es 1 pasajero")
    @Column("capacity")
    private Integer capacity;

    @Column("has_wifi")
    private Boolean hasWifi = false;

    @Column("has_ac")
    private Boolean hasAc = true;

    @Column("has_usb_chargers")
    private Boolean hasUsbChargers = false;

    @Column("has_toilet")
    private Boolean hasToilet = false;

    @Column("has_wheelchair_access")
    private Boolean hasWheelchairAccess = false;

    @Column("has_luggage_compartment")
    private Boolean hasLuggageCompartment = true;

    @Column("has_entertainment_system")
    private Boolean hasEntertainmentSystem = false;

    @Size(max = 50, message = "El tipo de asiento no puede exceder 50 caracteres")
    @Column("seat_type")
    private String seatType = "standard"; // standard, premium, vip

    @Column("description")
    private String description;

    @DecimalMin(value = "0.0", message = "El precio por km no puede ser negativo")
    @Column("price_per_km")
    private BigDecimal pricePerKm;

    @NotNull
    @Column("is_active")
    private Boolean active = true;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    /**
     * Verifica si el bus puede acomodar un número específico de pasajeros.
     */
    public boolean canAccommodate(int passengers) {
        return this.capacity != null && this.capacity >= passengers;
    }

    /**
     * Calcula el precio estimado para una distancia dada.
     */
    public BigDecimal calculatePrice(double distanceKm) {
        if (pricePerKm == null) {
            return BigDecimal.ZERO;
        }
        return pricePerKm.multiply(BigDecimal.valueOf(distanceKm));
    }

    /**
     * Constructor de conveniencia para crear un tipo de bus básico.
     */
    public BusType(UUID companyId, String name, Integer capacity, String seatType) {
        this.companyId = companyId;
        this.name = name;
        this.capacity = capacity;
        this.seatType = seatType;
        this.active = true;
    }
}
