package com.busconnect.apigateway.config;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Configuration
public class GatewayConfig {

    @Bean
    @Order(-1)
    public GlobalFilter requestTracingFilter() {
        return (exchange, chain) -> {
            String correlationId = exchange.getRequest().getHeaders()
                    .getFirst("X-Correlation-ID");

            if (correlationId == null) {
                correlationId = UUID.randomUUID().toString();
            }

            ServerHttpRequest request = exchange.getRequest().mutate()
                    .header("X-Correlation-ID", correlationId)
                    .build();

            String finalCorrelationId = correlationId;
            return chain.filter(exchange.mutate().request(request).build())
                    .then(Mono.fromRunnable(() -> {
                        exchange.getResponse().getHeaders()
                                .add("X-Correlation-ID", finalCorrelationId);
                    }));
        };
    }

    @Bean
    @Order(0)
    public GlobalFilter loggingFilter() {
        return (exchange, chain) -> {
            long startTime = System.currentTimeMillis();
            String path = exchange.getRequest().getPath().value();
            String method = exchange.getRequest().getMethod().name();

            return chain.filter(exchange)
                    .then(Mono.fromRunnable(() -> {
                        long duration = System.currentTimeMillis() - startTime;
                        int statusCode = exchange.getResponse().getStatusCode() != null
                                ? exchange.getResponse().getStatusCode().value()
                                : 0;
                        System.out.printf("[GATEWAY] %s %s -> %d (%dms)%n",
                                method, path, statusCode, duration);
                    }));
        };
    }
}
