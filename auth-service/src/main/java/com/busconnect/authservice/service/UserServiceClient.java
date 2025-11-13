package com.busconnect.authservice.service;

import com.busconnect.authservice.model.AuthUserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Cliente Feign para comunicarse con el microservicio 'user-service'.
 * El 'name' debe coincidir con el registro en Eureka.
 */
@FeignClient
public interface UserServiceClient {
    /**
     * Obtiene los datos de seguridad (hash de contraseña, estado, etc.)
     * del usuario mediante su email.
     * @param email Email del usuario a buscar.
     * @return El DTO AuthUserDTO con los datos de seguridad.
     */
    @GetMapping("/api/v1/users/security-data/{email}")
    AuthUserDTO getSecurityDataByEmail(@PathVariable("email") String email);

}
