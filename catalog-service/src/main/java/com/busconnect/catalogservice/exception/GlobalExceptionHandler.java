package com.busconnect.catalogservice.exception;

import com.busconnect.catalogservice.dto.response.ErrorResponse;
import com.busconnect.catalogservice.dto.response.ValidationErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Manejador global de excepciones para catalog-service.
 * 
 * ✅ CORRECCIÓN 7: Logging seguro sin información sensible
 * 
 * Principios aplicados:
 * - No loguear stack traces completos al usuario (solo en logs del servidor)
 * - Mensajes genéricos al usuario, detalles en logs
 * - Logs a nivel apropiado (WARN para errores de negocio, ERROR para técnicos)
 * - Sin información sensible en logs (coordenadas, IDs de usuario, etc.)
 * - Internacionalización con MessageSource
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Maneja excepciones de límite de tasa excedido de OpenRouteService.
     * 
     * ✅ CORRECCIÓN 7: Log a nivel WARN (es un error esperado de negocio, no técnico)
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitExceededException ex) {
        // ✅ Log sin información sensible (no incluir usuario ni detalles internos)
        log.warn("Rate limit exceeded for OpenRouteService API");
        
        String message = messageSource.getMessage(
            "openroute.rate.limit.exceeded", 
            null, 
            "Daily API rate limit has been exceeded. Please try again later.", 
            getCurrentLocale()
        );
                
        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .body(new ErrorResponse("RATE_LIMIT_EXCEEDED", message));
    }

    /**
     * Maneja excepciones cuando no se encuentra un municipio.
     * 
     * ✅ CORRECCIÓN 7: Log a nivel INFO (es un caso de negocio normal, no un error)
     */
    @ExceptionHandler(MunicipalityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMunicipalityNotFound(MunicipalityNotFoundException ex) {
        // ✅ Log a nivel INFO porque es un caso de negocio esperado
        // Municipios son datos públicos, OK loguearlos
        log.info("Municipality not found in database: {}", ex.getMunicipality());
        
        String message = messageSource.getMessage(
            "openroute.municipality.not.found", 
            new Object[]{ex.getMunicipality()}, 
            String.format("Municipality '%s' not found in Catalunya database", ex.getMunicipality()), 
            getCurrentLocale()
        );
                
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("MUNICIPALITY_NOT_FOUND", message));
    }

    /**
     * Maneja errores de validación de Bean Validation (@Valid).
     * 
     * ✅ CORRECCIÓN 7: Log sin datos del usuario, solo cantidad de errores
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        // ✅ Log solo la cantidad de errores, no los valores enviados por el usuario
        log.warn("Validation failed for {} field(s)", errors.size());
        
        return ResponseEntity
            .badRequest()
            .body(new ValidationErrorResponse(
                "VALIDATION_ERROR",
                "Request validation failed. Please check the error details.",
                errors
            ));
    }

    /**
     * Maneja errores de validación en WebFlux (reactivo).
     * Similar a MethodArgumentNotValidException pero para contextos reactivos.
     */
    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ValidationErrorResponse> handleWebExchangeBindException(
            WebExchangeBindException ex) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.warn("WebFlux validation failed for {} field(s)", errors.size());
        
        return ResponseEntity
            .badRequest()
            .body(new ValidationErrorResponse(
                "VALIDATION_ERROR",
                "Request validation failed. Please check the error details.",
                errors
            ));
    }

    /**
     * Maneja errores de argumentos ilegales (IllegalArgumentException).
     * Usado cuando se pasan parámetros inválidos a métodos.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        // ✅ Log del mensaje de error pero no stack trace (no es crítico)
        log.warn("Invalid argument provided: {}", ex.getMessage());
        
        return ResponseEntity
            .badRequest()
            .body(new ErrorResponse(
                "INVALID_ARGUMENT",
                ex.getMessage()  // El mensaje ya es seguro (viene de nuestro código)
            ));
    }

    /**
     * Maneja excepciones específicas de OpenRouteService.
     * Errores técnicos al comunicarse con la API externa.
     */
    @ExceptionHandler(OpenRouteServiceException.class)
    public ResponseEntity<ErrorResponse> handleOpenRouteServiceException(OpenRouteServiceException ex) {
        // ✅ Log a nivel ERROR porque es un problema técnico
        log.error("OpenRouteService API error: {}", ex.getMessage());
        
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ErrorResponse(
                "EXTERNAL_SERVICE_ERROR",
                "Route calculation service is temporarily unavailable. Please try again later."
            ));
    }

    /**
     * ✅ CORRECCIÓN 7: Maneja cualquier excepción no capturada específicamente.
     * 
     * IMPORTANTE:
     * - Stack trace completo solo en logs (servidor)
     * - Mensaje genérico al usuario (sin detalles técnicos)
     * - Log a nivel ERROR (es un problema técnico no esperado)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        // ✅ Log completo con stack trace SOLO en logs del servidor
        // El usuario NO ve esto, solo ve el mensaje genérico de abajo
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        
        // ✅ Mensaje genérico al usuario (sin exponer detalles internos)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred. Please try again later or contact support if the problem persists."
            ));
    }

    /**
     * Obtiene el Locale actual del contexto de la request.
     * Fallback a Locale por defecto si no hay contexto.
     * 
     * Útil para internacionalización de mensajes de error.
     */
    private Locale getCurrentLocale() {
        try {
            Locale locale = LocaleContextHolder.getLocale();
            return locale != null ? locale : Locale.getDefault();
        } catch (Exception e) {
            // Fallback si falla la obtención del locale
            return Locale.getDefault();
        }
    }
}