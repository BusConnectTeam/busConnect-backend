package com.busconnect.catalogservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteResult {
    private String origin;
    private String destination;
    private BigDecimal distanceKm;
    private Integer durationMinutes;
    private String source; // "openroute", "fallback", "cache"
    private boolean success;
}