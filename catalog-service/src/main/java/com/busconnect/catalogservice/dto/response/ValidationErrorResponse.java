package com.busconnect.catalogservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO para respuestas de error de validación.
 * Incluye detalles específicos sobre cada campo que falló la validación.
 * 
 * @param code Código del error de validación (siempre VALIDATION_ERROR)
 * @param message Mensaje general de validación
 * @param errors Mapa de campo -> mensaje de error específico
 * @param timestamp Momento exacto en que ocurrió el error
 */
@Schema(description = "Respuesta de error con detalles de validación")
public record ValidationErrorResponse(
    
    @Schema(description = "Código del error", example = "VALIDATION_ERROR")
    String code,
    
    @Schema(description = "Mensaje general", example = "Validation failed")
    String message,
    
    @Schema(description = "Errores por campo", example = "{\"originMunicipality\": \"Municipality name is required\"}")
    Map<String, String> errors,
    
    @Schema(description = "Timestamp del error", example = "2024-01-15T10:30:00")
    LocalDateTime timestamp
) {
    /**
     * Constructor de conveniencia que establece el timestamp automáticamente.
     */
    public ValidationErrorResponse(String code, String message, Map<String, String> errors) {
        this(code, message, errors, LocalDateTime.now());
    }
}