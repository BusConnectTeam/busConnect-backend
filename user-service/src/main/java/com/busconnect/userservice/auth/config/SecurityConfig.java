package com.busconnect.userservice.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Security configuration for the reactive application.
 * Configures security filter chain and password encoding.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * Configures the security filter chain.
     * Disables CSRF, HTTP Basic, and Form Login.
     * Configures public access to /auth/** and requires authentication for other
     * endpoints.
     *
     * @param http the ServerHttpSecurity object
     * @return the configured SecurityWebFilterChain
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // Permitir acceso público a endpoints de autenticación
                        .pathMatchers("/api/users/auth/**").permitAll()
                        // Bloquear cualquier otra petición
                        .anyExchange().authenticated())
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .build();
    }

    /**
     * Provides the password encoder bean.
     * Uses BCrypt for hashing passwords.
     *
     * @return the PasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
