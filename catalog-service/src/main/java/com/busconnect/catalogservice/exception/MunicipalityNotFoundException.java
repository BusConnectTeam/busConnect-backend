package com.busconnect.catalogservice.exception;

import lombok.Getter;

@Getter
public class MunicipalityNotFoundException extends RuntimeException {
    
    private final String municipality;
    
    public MunicipalityNotFoundException(String municipality) {
        super("Municipality not found: " + municipality);
        this.municipality = municipality;
    }
    
    public MunicipalityNotFoundException(String municipality, String message) {
        super(message);
        this.municipality = municipality;
    }
}