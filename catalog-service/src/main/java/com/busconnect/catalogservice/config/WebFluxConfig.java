package com.busconnect.catalogservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * Configuración de Spring WebFlux para el Catalog Service.
 * Asegura que los controladores REST se mapeen correctamente.
 */
@Configuration
@EnableWebFlux
public class WebFluxConfig implements WebFluxConfigurer {
    // Configuración por defecto es suficiente
}
