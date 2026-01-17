package com.busconnect.apigateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/catalog")
    public Mono<ResponseEntity<Map<String, Object>>> catalogFallback() {
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", "error",
                        "service", "catalog-service",
                        "message", "Catalog service is temporarily unavailable. Please try again later.",
                        "timestamp", LocalDateTime.now().toString()
                )));
    }

    @GetMapping("/users")
    public Mono<ResponseEntity<Map<String, Object>>> usersFallback() {
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", "error",
                        "service", "user-service",
                        "message", "User service is temporarily unavailable. Please try again later.",
                        "timestamp", LocalDateTime.now().toString()
                )));
    }
}
