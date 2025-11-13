package com.busconnect.authservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO (Data Transfer Object) que representa los datos de seguridad esenciales
 * del usuario transferidos desde el 'user-service' (Persistencia) al 'auth-service'.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserDTO {

    private String email;
    private String passwordHash;
    private int failedAttempt;
    private Boolean accountNotLocked;


}
