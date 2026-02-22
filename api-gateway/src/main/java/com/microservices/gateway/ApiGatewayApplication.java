package com.microservices.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway Application
 * 
 * This service acts as the single entry point for all client requests.
 * 
 * Key responsibilities:
 * 1. Route requests to appropriate microservices using logical service names
 * 2. Validate JWT tokens for protected endpoints
 * 3. Implement circuit breaker pattern for resilience
 * 4. Handle CORS configuration
 * 5. Provide fallback responses when services are unavailable
 * 
 * All requests flow through this gateway:
 * - /api/auth/** -> auth-service (Public)
 * - /api/users/** -> user-service (Protected)
 * - /api/loans/** -> loan-service (Protected)
 * 
 * @author Spring Microservices Team
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
