package com.busconnect.catalogservice.repository;

import com.busconnect.catalogservice.model.Schedule;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Repository
public interface ScheduleRepository extends R2dbcRepository<Schedule, UUID> {

    /**
     * Buscar horarios por ruta específica
     */
    @Query("SELECT * FROM catalog.schedules " +
           "WHERE route_id = :routeId " +
           "AND status = 'AVAILABLE' " +
           "ORDER BY departure_time")
    Flux<Schedule> findByRouteId(@Param("routeId") UUID routeId);

    /**
     * Buscar horarios por empresa
     */
    @Query("SELECT * FROM catalog.schedules " +
           "WHERE company_id = :companyId " +
           "AND status IN ('AVAILABLE', 'FULL') " +
           "ORDER BY departure_time")
    Flux<Schedule> findByCompanyId(@Param("companyId") UUID companyId);

    /**
     * Buscar horarios por tipo de vehículo
     */
    @Query("SELECT * FROM catalog.schedules " +
           "WHERE vehicle_type_id = :vehicleTypeId " +
           "AND status = 'AVAILABLE' " +
           "ORDER BY departure_time")
    Flux<Schedule> findByVehicleTypeId(@Param("vehicleTypeId") UUID vehicleTypeId);

    /**
     * Buscar horarios disponibles para una fecha específica
     */
    @Query("SELECT * FROM catalog.schedules " +
           "WHERE :date = ANY(available_dates) " +
           "AND status = 'AVAILABLE' " +
           "AND available_seats > 0 " +
           "ORDER BY departure_time")
    Flux<Schedule> findAvailableForDate(@Param("date") LocalDate date);

    /**
     * Buscar horarios por ruta y fecha
     */
    @Query("SELECT * FROM catalog.schedules " +
           "WHERE route_id = :routeId " +
           "AND :date = ANY(available_dates) " +
           "AND status = 'AVAILABLE' " +
           "AND available_seats >= :passengers " +
           "ORDER BY departure_time")
    Flux<Schedule> findByRouteAndDateWithCapacity(@Param("routeId") UUID routeId,
                                                 @Param("date") LocalDate date,
                                                 @Param("passengers") Integer passengers);

    /**
     * Buscar horarios en un rango de tiempo
     */
    @Query("SELECT * FROM catalog.schedules " +
           "WHERE departure_time BETWEEN :startTime AND :endTime " +
           "AND status = 'AVAILABLE' " +
           "ORDER BY departure_time")
    Flux<Schedule> findByTimeRange(@Param("startTime") LocalTime startTime,
                                  @Param("endTime") LocalTime endTime);

    /**
     * Buscar horarios con asientos suficientes
     */
    @Query("SELECT * FROM catalog.schedules " +
           "WHERE available_seats >= :passengers " +
           "AND status = 'AVAILABLE' " +
           "ORDER BY departure_time")
    Flux<Schedule> findWithAvailableSeats(@Param("passengers") Integer passengers);

    /**
     * Buscar horarios por conductor asignado
     */
    @Query("SELECT * FROM catalog.schedules " +
           "WHERE driver_id = :driverId " +
           "ORDER BY departure_time")
    Flux<Schedule> findByDriverId(@Param("driverId") UUID driverId);

    /**
     * Búsqueda compleja: ruta, fecha, pasajeros y empresa
     */
    @Query("SELECT * FROM catalog.schedules " +
           "WHERE route_id = :routeId " +
           "AND company_id = :companyId " +
           "AND :date = ANY(available_dates) " +
           "AND status = 'AVAILABLE' " +
           "AND available_seats >= :passengers " +
           "ORDER BY departure_time")
    Flux<Schedule> findAvailableSchedules(@Param("routeId") UUID routeId,
                                        @Param("companyId") UUID companyId,
                                        @Param("date") LocalDate date,
                                        @Param("passengers") Integer passengers);

    /**
     * Contar horarios disponibles para una ruta y fecha
     */
    @Query("SELECT COUNT(*) FROM catalog.schedules " +
           "WHERE route_id = :routeId " +
           "AND :date = ANY(available_dates) " +
           "AND status = 'AVAILABLE'")
    Mono<Long> countAvailableForRouteAndDate(@Param("routeId") UUID routeId,
                                           @Param("date") LocalDate date);

    /**
     * Buscar horarios que necesitan conductor asignado
     */
    @Query("SELECT * FROM catalog.schedules " +
           "WHERE driver_id IS NULL " +
           "AND status = 'AVAILABLE' " +
           "ORDER BY departure_time")
    Flux<Schedule> findWithoutAssignedDriver();
}