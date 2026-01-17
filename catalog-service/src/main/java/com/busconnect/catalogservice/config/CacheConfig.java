package com.busconnect.catalogservice.config;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.busconnect.catalogservice.dto.response.RouteResultResponse;
import com.busconnect.catalogservice.model.Municipality;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.extern.slf4j.Slf4j;

/**
 * Configuración de caché reactivo con Caffeine.
 *
 * Este caché es COMPARTIDO entre todos los usuarios, lo que significa que:
 * - Si Usuario A busca "Barcelona → Girona", se llama a OpenRouteService API
 * - Si Usuario B busca "Barcelona → Girona", se devuelve el resultado cacheado
 * - Ahorro de llamadas a API = Ahorro de dinero
 *
 * El caché es thread-safe y compatible con programación reactiva.
 */
@Configuration
@Slf4j
public class CacheConfig {

    @Value("${cache.routes.maximum-size:1000}")
    private int routesMaximumSize;

    @Value("${cache.routes.expire-after-write-hours:1}")
    private int routesExpireAfterWriteHours;

    @Value("${cache.municipalities.maximum-size:1000}")
    private int municipalitiesMaximumSize;

    @Value("${cache.municipalities.expire-after-write-hours:24}")
    private int municipalitiesExpireAfterWriteHours;

    /**
     * Caché de rutas calculadas.
     *
     * Key: "origin-destination" (e.g., "barcelona-girona")
     * Value: RouteResultResponse con distancia, duración, etc.
     *
     * Configuración por defecto:
     * - Máximo 1000 rutas cacheadas
     * - Expiran después de 1 hora
     * - Estadísticas habilitadas para monitoreo
     */
    @Bean
    public Cache<String, RouteResultResponse> routeCache() {
        Cache<String, RouteResultResponse> cache = Caffeine.newBuilder()
                .maximumSize(routesMaximumSize)
                .expireAfterWrite(routesExpireAfterWriteHours, TimeUnit.HOURS)
                .recordStats()
                .build();

        log.info("Route cache initialized: maxSize={}, expireAfterWrite={}h",
                routesMaximumSize, routesExpireAfterWriteHours);

        return cache;
    }

    /**
     * Caché de municipios.
     *
     * Key: nombre normalizado del municipio (e.g., "barcelona")
     * Value: Municipality con coordenadas, provincia, etc.
     *
     * Configuración por defecto:
     * - Máximo 1000 municipios (Catalunya tiene 947)
     * - Expiran después de 24 horas (los municipios no cambian frecuentemente)
     * - Estadísticas habilitadas para monitoreo
     */
    @Bean
    public Cache<String, Municipality> municipalityCache() {
        Cache<String, Municipality> cache = Caffeine.newBuilder()
                .maximumSize(municipalitiesMaximumSize)
                .expireAfterWrite(municipalitiesExpireAfterWriteHours, TimeUnit.HOURS)
                .recordStats()
                .build();

        log.info("Municipality cache initialized: maxSize={}, expireAfterWrite={}h",
                municipalitiesMaximumSize, municipalitiesExpireAfterWriteHours);

        return cache;
    }
}
