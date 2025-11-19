package com.busconnect.catalogservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * DTO para respuestas de error simples.
 * Utilizado para comunicar errores de negocio y técnicos al cliente.
 * 
 * @param code Código único del error (ej: MUNICIPALITY_NOT_FOUND)
 * @param message Mensaje descriptivo del error
 * @param timestamp Momento exacto en que ocurrió el error
 */
@Schema(description = "Respuesta estándar de error")
public record ErrorResponse(
    
    @Schema(description = "Código único del error", example = "MUNICIPALITY_NOT_FOUND")
    String code,
    
    @Schema(description = "Mensaje descriptivo del error", example = "Municipality 'Barcelona' not found")
    String message,
    
    @Schema(description = "Timestamp del error", example = "2024-01-15T10:30:00")
    LocalDateTime timestamp
) {
    /**
     * Constructor de conveniencia que establece el timestamp automáticamente.
     */
    public ErrorResponse(String code, String message) {
        this(code, message, LocalDateTime.now());
    }
}