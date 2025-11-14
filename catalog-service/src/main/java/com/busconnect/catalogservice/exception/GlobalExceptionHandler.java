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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Manejador global de excepciones para catalog-service.
 * Centraliza el manejo de errores y proporciona respuestas consistentes.
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
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitExceededException ex) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());
        
        String message = messageSource.getMessage(
            "openroute.rate.limit.exceeded", 
            null, 
            ex.getMessage(), 
            getCurrentLocale()
        );
                
        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .body(new ErrorResponse("RATE_LIMIT_EXCEEDED", message));
    }

    /**
     * Maneja excepciones cuando no se encuentra un municipio.
     */
    @ExceptionHandler(MunicipalityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMunicipalityNotFound(MunicipalityNotFoundException ex) {
        log.info("Municipality not found: {}", ex.getMunicipality());
        
        String message = messageSource.getMessage(
            "openroute.municipality.not.found", 
            new Object[]{ex.getMunicipality()}, 
            ex.getMessage(), 
            getCurrentLocale()
        );
                
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("MUNICIPALITY_NOT_FOUND", message));
    }

    /**
     * Maneja errores de validación de Bean Validation.
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
        
        log.warn("Validation error: {}", errors);
        
        return ResponseEntity
            .badRequest()
            .body(new ValidationErrorResponse(
                "VALIDATION_ERROR",
                "Validation failed",
                errors
            ));
    }

/**
 * Maneja cualquier excepción no capturada específicamente.
 */
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
    log.error("Unexpected error: {}", ex.getMessage(), ex);  
    
    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorResponse(
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred"
        ));
}

    /**
     * Obtiene el Locale actual del contexto de la request.
     * Fallback a Locale.getDefault() si no hay contexto.
     */
    private Locale getCurrentLocale() {
        Locale locale = LocaleContextHolder.getLocale();
        return locale != null ? locale : Locale.getDefault();
    }
}
