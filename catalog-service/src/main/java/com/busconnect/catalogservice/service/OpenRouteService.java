package com.busconnect.catalogservice.service;

import com.busconnect.catalogservice.dto.response.RouteResult;
import com.busconnect.catalogservice.exception.RateLimitExceededException;
import com.busconnect.catalogservice.exception.MunicipalityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
@Slf4j
public class OpenRouteService {

    private final WebClient webClient;
    private final String apiKey;
    private final int maxRequestsPerDay;
    private int dailyRequestCount = 0;
    
    public OpenRouteService(
            @Value("${openroute.api.key}") String apiKey,
            @Value("${openroute.api.base-url}") String baseUrl,
            @Value("${openroute.api.max-requests-per-day:2000}") int maxRequestsPerDay
    ) {
        this.apiKey = apiKey;
        this.maxRequestsPerDay = maxRequestsPerDay;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", apiKey)
                .build();
    }

    public Mono<RouteResult> calculateRoute(String originCity, String destinationCity) {
        if (dailyRequestCount >= maxRequestsPerDay) {
            log.warn("Rate limit exceeded for OpenRouteService");
            return Mono.error(new RateLimitExceededException("Daily API limit exceeded"));
        }

        log.info("Calculating route: {} -> {}", originCity, destinationCity);
        
        return Mono.fromCallable(() -> {
            dailyRequestCount++;
            
            // Mock data para Barcelona -> Sitges
            if ("barcelona".equalsIgnoreCase(originCity) && "sitges".equalsIgnoreCase(destinationCity)) {
                return RouteResult.builder()
                        .origin(originCity)
                        .destination(destinationCity)
                        .distanceKm(new BigDecimal("42.3"))
                        .durationMinutes(38)
                        .source("openroute")
                        .success(true)
                        .build();
            }
            
            throw new MunicipalityNotFoundException("Municipio no encontrado: " + originCity + " o " + destinationCity);
        });
    }
}