package com.microservices.loan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Loan Service Application
 * 
 * This service provides loan information for users.
 * 
 * Features:
 * 1. Get loan details by user ID (hardcoded responses)
 * 2. Role-based access control (users can only access their own loans, admins can access all)
 * 3. Circuit breaker pattern for resilience
 * 
 * Protected endpoints - requires JWT token from API Gateway
 * 
 * @author Spring Microservices Team
 */
@SpringBootApplication
public class LoanServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoanServiceApplication.class, args);
    }
}
