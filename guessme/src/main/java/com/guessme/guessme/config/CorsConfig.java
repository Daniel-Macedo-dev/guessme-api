package com.guessme.guessme.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Central CORS configuration. Allowed origins are read from:
 *   cors.allowed-origins  (application.properties, default: http://localhost:5173)
 *
 * Accepts a comma-separated list, e.g.:
 *   cors.allowed-origins=http://localhost:5173,https://daniel-macedo-dev.github.io
 *
 * Override via CORS_ALLOWED_ORIGINS environment variable or in application.properties.
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration cors = new CorsConfiguration();

        Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .forEach(cors::addAllowedOrigin);

        cors.addAllowedHeader("*");
        cors.addAllowedMethod("*");
        cors.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);

        return new CorsWebFilter(source);
    }
}
