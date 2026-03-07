package com.loopy.config;

// Dependencies: CorsConfiguration, CorsConfigurationSource, UrlBasedCorsConfigurationSource — see DEPENDENCY_GUIDE.md
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS configuration — allows the React frontend (localhost:5173) to call the backend API.
 * Without this, browsers block cross-origin requests from the frontend dev server.
 * Update allowedOrigins when deploying to production.
 *
 * CORS (Cross-Origin Resource Sharing) is a browser security mechanism. When the frontend
 * at localhost:5173 makes a request to the backend at localhost:8080, the browser considers
 * this "cross-origin" (different port = different origin). The browser blocks the response
 * unless the backend explicitly allows that origin via CORS headers in the response.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
