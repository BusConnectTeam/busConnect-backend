package com.busconnect.catalogservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "schedules", schema = "catalog")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull(message = "Route is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @NotNull(message = "Company is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @NotNull(message = "Vehicle type is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_type_id", nullable = false)
    private VehicleType vehicleType;

    // Horarios
    @NotNull(message = "Departure time is required")
    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;

    @Column(name = "arrival_time")
    private LocalTime arrivalTime; // Calculado automáticamente

    // Disponibilidad
    @Column(name = "available_dates", columnDefinition = "date[]")
    private LocalDate[] availableDates; // Array de fechas disponibles

    @NotNull(message = "Total seats is required")
    @Min(value = 1, message = "Total seats must be at least 1")
    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    @NotNull(message = "Available seats is required")
    @Min(value = 0, message = "Available seats cannot be negative")
    @Column(name = "available_seats", nullable = false)
    private Integer availableSeats;

    // Estado
    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduleStatus status = ScheduleStatus.AVAILABLE;

    // Driver info (para Fase 2)
    @Column(name = "driver_id")
    private UUID driverId;

    @Column(name = "driver_assigned_at")
    private LocalDateTime driverAssignedAt;

    // Control de concurrencia (Optimistic Locking)
    @Version
    private Integer version = 1;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Enum para estados
    public enum ScheduleStatus {
        AVAILABLE,  // Disponible para reservar
        FULL,       // Sin asientos disponibles
        CANCELLED   // Cancelado por la empresa
    }

    // Método helper para verificar disponibilidad en una fecha
    public boolean isAvailableOn(LocalDate date) {
        if (availableDates == null || status != ScheduleStatus.AVAILABLE) {
            return false;
        }
        
        for (LocalDate availableDate : availableDates) {
            if (availableDate.equals(date)) {
                return true;
            }
        }
        return false;
    }

    // Método helper para verificar si puede acomodar pasajeros
    public boolean canAccommodate(int passengers) {
        return status == ScheduleStatus.AVAILABLE && 
               availableSeats >= passengers;
    }

    // Método helper para reservar asientos
    public boolean reserveSeats(int passengers) {
        if (!canAccommodate(passengers)) {
            return false;
        }
        
        availableSeats -= passengers;
        
        // Marcar como FULL si no quedan asientos
        if (availableSeats == 0) {
            status = ScheduleStatus.FULL;
        }
        
        return true;
    }
}