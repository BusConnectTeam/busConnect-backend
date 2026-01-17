package com.busconnect.catalogservice.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuración de OpenRouteService API.
 *
 * Propiedades validadas al iniciar la aplicación (fail-fast).
 * Si falta la API key o la URL es inválida, la aplicación no arranca.
 */
@Configuration
@ConfigurationProperties(prefix = "openroute.api")
@Validated
@Data
public class OpenRouteProperties {

    /**
     * API key de OpenRouteService (obligatoria).
     */
    @NotBlank(message = "OpenRouteService API key is required. Set OPENROUTE_API_KEY environment variable.")
    private String key;

    /**
     * URL base de la API (debe ser HTTPS en producción).
     */
    @NotBlank(message = "OpenRouteService base URL is required")
    @Pattern(regexp = "https?://.*", message = "Base URL must be a valid HTTP/HTTPS URL")
    private String baseUrl;

    /**
     * Timeout para requests a la API (default: 20s).
     */
    private Duration timeout = Duration.ofSeconds(20);

    /**
     * Configuración de rate limiting.
     */
    @Valid
    private RateLimit rateLimit = new RateLimit();

    @Data
    public static class RateLimit {
        /**
         * Máximo de requests permitidos por día (default: 2000).
         */
        @Min(value = 1, message = "Max requests per day must be at least 1")
        private int maxRequestsPerDay = 2000;
    }
}