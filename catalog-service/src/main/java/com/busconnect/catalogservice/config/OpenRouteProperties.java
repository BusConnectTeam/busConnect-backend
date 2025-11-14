package com.busconnect.catalogservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "openroute.api")
@Data
public class OpenRouteProperties {
    
    private String key;
    private String baseUrl;
    private Duration timeout = Duration.ofSeconds(10);
    private RateLimit rateLimit = new RateLimit();
    
    @Data
    public static class RateLimit {
        private int maxRequestsPerDay = 2000;
    }
}