package com.loopy.auth.filter;

import com.loopy.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Intercepts every HTTP request to check for a JWT in the Authorization header.
 * If a valid token is found, it loads the user and sets the SecurityContext
 * so downstream code (controllers, @AuthenticationPrincipal) can access the user.
 *
 * Extends OncePerRequestFilter to guarantee it runs exactly once per request.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Look for "Authorization: Bearer <token>" header
        String authHeader = request.getHeader("Authorization");

        // No token — skip authentication, let the request continue (may hit a public endpoint)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Strip "Bearer " prefix to get the raw JWT
        String token = authHeader.substring(7);

        try {
            String email = jwtService.extractEmail(token);

            // Only authenticate if no existing auth in context (avoid re-authenticating)
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                var userDetails = userDetailsService.loadUserByUsername(email);

                if (jwtService.isTokenValid(token, userDetails)) {
                    // Set the authenticated user in SecurityContext so controllers can access it
                    var authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception ignored) {
            // Invalid/expired/malformed token — continue without authentication
            // The request will be rejected by Spring Security if it hits a protected endpoint
        }

        filterChain.doFilter(request, response);
    }
}
