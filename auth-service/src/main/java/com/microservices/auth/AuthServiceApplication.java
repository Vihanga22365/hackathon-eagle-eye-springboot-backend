package com.microservices.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Authentication and Authorization Service
 * 
 * This service is responsible for:
 * 1. User authentication using Firebase Authentication (Email & Password)
 * 2. JWT token generation and validation
 * 3. Role-based authorization (CUSTOMER, SYSTEM_ADMIN)
 * 4. User registration and login management
 * 
 * The service integrates with Firebase Authentication for user management
 * and issues JWT tokens that can be validated by other services via API Gateway.
 * 
 * @author Spring Microservices Team
 */
@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
