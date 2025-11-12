package com.busconnect.catalogservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitExceededException ex) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());
        
        String message = messageSource.getMessage("openroute.rate.limit.exceeded", 
                null, ex.getMessage(), Locale.getDefault());
                
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ErrorResponse("RATE_LIMIT_EXCEEDED", message));
    }

    @ExceptionHandler(MunicipalityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMunicipalityNotFound(MunicipalityNotFoundException ex) {
        log.info("Municipality not found: {}", ex.getMunicipality());
        
        String message = messageSource.getMessage("openroute.municipality.not.found", 
                new Object[]{ex.getMunicipality()}, ex.getMessage(), Locale.getDefault());
                
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("MUNICIPALITY_NOT_FOUND", message));
    }

    // DTO para respuestas de error
    public static class ErrorResponse {
        public String code;
        public String message;
        
        public ErrorResponse(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
