package com.microservices.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * User Service Application
 * 
 * This service manages user profile information using Firebase Realtime Database.
 * 
 * Features:
 * 1. Create/Update/Read user profiles
 * 2. Role-based access control (users can only access their own profile, admins can access all)
 * 3. Integration with Firebase Realtime Database
 * 4. Circuit breaker pattern for resilience
 * 
 * Protected endpoints - requires JWT token from API Gateway
 * 
 * @author Spring Microservices Team
 */
@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
