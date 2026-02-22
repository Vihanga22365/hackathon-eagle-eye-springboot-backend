package com.microservices.gateway.filter;

import com.microservices.gateway.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Authentication Filter
 * 
 * This filter validates JWT tokens for protected routes.
 * It extracts the token from the Authorization header,
 * validates it, and adds user information to request headers
 * for downstream services.
 * 
 * Flow:
 * 1. Extract Authorization header
 * 2. Validate JWT token
 * 3. Extract user details (userId, email, role)
 * 4. Add headers for downstream services
 * 5. Allow request to proceed or return 401 Unauthorized
 */
@Component
@Slf4j
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Autowired
    private JwtUtil jwtUtil;

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            if (HttpMethod.OPTIONS.equals(request.getMethod())
                    || path.endsWith("/health")
                    || path.contains("/actuator/health")) {
                return chain.filter(exchange);
            }

            log.info("Authenticating request: {} {}", request.getMethod(), request.getURI());

            // Check if Authorization header is present
            if (!request.getHeaders().containsKey("Authorization")) {
                log.warn("Missing Authorization header");
                return onError(exchange, "Missing Authorization header", HttpStatus.UNAUTHORIZED);
            }

            // Extract token
            String authHeader = request.getHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Invalid Authorization header format");
                return onError(exchange, "Invalid Authorization header format", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            // Validate token
            try {
                if (!jwtUtil.validateToken(token)) {
                    log.warn("Invalid or expired token");
                    return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
                }

                // Extract user information
                String userId = jwtUtil.extractUserId(token);
                String email = jwtUtil.extractEmail(token);
                String role = jwtUtil.extractRole(token);

                log.info("Token validated for user: {} with role: {}", email, role);

                // Add user information to request headers for downstream services
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-User-Id", userId)
                        .header("X-User-Email", email)
                        .header("X-User-Role", role)
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (Exception e) {
                log.error("Token validation error: {}", e.getMessage());
                return onError(exchange, "Token validation failed", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    /**
     * Handle authentication errors
     */
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        if (status == HttpStatus.UNAUTHORIZED) {
            log.warn("Authentication error: {}", message);
        } else {
            log.error("Authentication error: {}", message);
        }
        return response.setComplete();
    }

    /**
     * Configuration class for the filter
     */
    public static class Config {
        // Configuration properties can be added here if needed
    }
}
