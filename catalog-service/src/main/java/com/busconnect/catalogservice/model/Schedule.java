package com.busconnect.catalogservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "schedules", schema = "catalog")
public class Schedule {

    @Id
    private UUID id;

    // En R2DBC usamos IDs directamente en lugar de @ManyToOne
    @NotNull(message = "{schedule.route.required}")
    @Column("route_id")
    private UUID routeId;

    @NotNull(message = "{schedule.company.required}")
    @Column("company_id")
    private UUID companyId;

    @NotNull(message = "{schedule.vehicle.type.required}")
    @Column("vehicle_type_id")
    private UUID vehicleTypeId;

    // Horarios
    @NotNull(message = "{schedule.departure.time.required}")
    @Column("departure_time")
    private LocalTime departureTime;

    @Column("arrival_time")
    private LocalTime arrivalTime; // Calculado automáticamente

    // Disponibilidad (PostgreSQL array de fechas)
    @Column("available_dates")
    private LocalDate[] availableDates; // Array de fechas disponibles

    @NotNull(message = "{schedule.total.seats.required}")
    @Min(value = 1, message = "{schedule.total.seats.min}")
    @Column("total_seats")
    private Integer totalSeats;

    @NotNull(message = "{schedule.available.seats.required}")
    @Min(value = 0, message = "{schedule.available.seats.min}")
    @Column("available_seats")
    private Integer availableSeats;

    // Estado
    @NotNull(message = "{schedule.status.required}")
    @Column("status")
    private ScheduleStatus status = ScheduleStatus.AVAILABLE;

    // Driver info (para Fase 2)
    @Column("driver_id")
    private UUID driverId;

    @Column("driver_assigned_at")
    private LocalDateTime driverAssignedAt;

    // Control de concurrencia (Optimistic Locking)
    @Version
    private Integer version = 1;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    // Enum para estados
    public enum ScheduleStatus {
        AVAILABLE,  // Disponible para reservar
        FULL,       // Sin asientos disponibles
        CANCELLED   // Cancelado por la empresa
    }

    // Constructor conveniente
    public Schedule(UUID routeId, UUID companyId, UUID vehicleTypeId, 
                   LocalTime departureTime, Integer totalSeats) {
        this.routeId = routeId;
        this.companyId = companyId;
        this.vehicleTypeId = vehicleTypeId;
        this.departureTime = departureTime;
        this.totalSeats = totalSeats;
        this.availableSeats = totalSeats; // Inicialmente todos disponibles
        this.status = ScheduleStatus.AVAILABLE;
        this.version = 1;
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

    // Método helper para calcular arrival time basado en route duration
    public void calculateArrivalTime(Integer routeDurationMinutes) {
        if (departureTime != null && routeDurationMinutes != null) {
            this.arrivalTime = departureTime.plusMinutes(routeDurationMinutes);
        }
    }
}