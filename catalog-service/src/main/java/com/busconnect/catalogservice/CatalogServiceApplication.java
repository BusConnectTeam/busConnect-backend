package com.busconnect.catalogservice;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@OpenAPIDefinition(
        info = @Info(
                title = "Catalog Service API",
                version = "1.0.0",
                description = "Microservicio reactivo para gestión de rutas y búsquedas de autobuses en Catalunya"
        )
)
@EnableScheduling
@EnableR2dbcAuditing  // Para @CreatedDate/@LastModifiedDate en R2DBC
@EnableCaching       // Para cache de rutas OpenRouteService
public class CatalogServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogServiceApplication.class, args);
    }
}