package com.busconnect.catalogservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteResultResponse {

    private String origin;
    private String destination;
    private BigDecimal distanceKm;
    private Integer durationMinutes;
    private String source; // "openroute", "fallback", "cache"
    private boolean success;
    private String errorMessage;
    private LocalDateTime calculatedAt;

    // Constructor para resultados exitosos
    public RouteResultResponse(String origin, String destination, BigDecimal distanceKm, 
                              Integer durationMinutes, String source) {
        this.origin = origin;
        this.destination = destination;
        this.distanceKm = distanceKm;
        this.durationMinutes = durationMinutes;
        this.source = source;
        this.success = true;
        this.errorMessage = null;
        this.calculatedAt = LocalDateTime.now();
    }

    // Constructor para errores
    public RouteResultResponse(String origin, String destination, String errorMessage) {
        this.origin = origin;
        this.destination = destination;
        this.distanceKm = null;
        this.durationMinutes = null;
        this.source = "error";
        this.success = false;
        this.errorMessage = errorMessage;
        this.calculatedAt = LocalDateTime.now();
    }
}