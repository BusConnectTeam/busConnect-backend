package com.busconnect.authservice.client;

import com.busconnect.authservice.client.UserServiceClient;
import com.busconnect.authservice.model.AuthUserDTO;
import org.springframework.stereotype.Service;

/**
 * Servicio principal para la lógica de autenticación y autorización.
 */
@Service
public class AuthService {

    private final UserServiceClient userServiceClient;

    // Inyección de dependencia del Feign Client (Constructor Injection)
    public AuthService(UserServiceClient userServiceClient) {
        this.userServiceClient = userServiceClient;
    }

    /**
     * Placeholder para probar la inyección y el cliente Feign.
     * En el futuro, este método contendrá la lógica principal de autenticación.
     */
    public AuthUserDTO authenticate(String email, String password) {
        // Lógica de prueba: solo llamamos al Feign Client
        // NOTA: Esta llamada fallará si Eureka y user-service están caídos, pero
        // la aplicación habrá arrancado si la inyección es correcta.

        System.out.println("DEBUG: Llamando a user-service para obtener datos de seguridad...");

        // Simplemente retornamos el resultado del Feign Client por ahora.
        return userServiceClient.getSecurityDataByEmail(email);
    }

}
