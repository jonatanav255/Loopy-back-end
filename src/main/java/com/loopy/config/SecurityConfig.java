package com.loopy.config;

import com.loopy.auth.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * HTTP security configuration — defines which endpoints are public vs protected,
 * disables CSRF (not needed for stateless JWT auth), and wires in the JWT filter.
 *
 * Separated from AppConfig to avoid circular dependency:
 *   SecurityConfig -> JwtAuthFilter -> UserDetailsService -> AppConfig (no cycle)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
                          AuthenticationProvider authenticationProvider,
                          CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.authenticationProvider = authenticationProvider;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // CSRF disabled — we use JWT tokens, not cookies, for auth
                .csrf(csrf -> csrf.disable())
                // Stateless — no server-side sessions, every request must carry a JWT
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // /me requires a valid JWT (must be listed BEFORE the wildcard)
                        .requestMatchers("/api/auth/me").authenticated()
                        // All other auth endpoints (register, login, refresh, logout) are public
                        .requestMatchers("/api/auth/**").permitAll()
                        // Everything else requires authentication
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider)
                // Run our JWT filter before Spring's default username/password filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
