package com.busconnect.catalogservice.exception;

public class OpenRouteServiceException extends RuntimeException {
    public OpenRouteServiceException(String message) {
        super(message);
    }
    
    public OpenRouteServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}