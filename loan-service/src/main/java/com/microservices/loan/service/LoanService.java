package com.microservices.loan.service;

import com.microservices.loan.model.Loan;
import com.microservices.loan.model.LoanResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Loan Service
 * 
 * Provides hardcoded loan data for demonstration purposes.
 * In a real application, this would fetch data from a database.
 */
@Service
@Slf4j
public class LoanService {

    /**
     * Get loans for a specific user
     * Returns hardcoded loan data
     */
    @CircuitBreaker(name = "loanService", fallbackMethod = "getLoansFallback")
    public LoanResponse getUserLoans(String userId) {
        log.info("Fetching loans for user: {}", userId);
        
        // Simulate data fetch - hardcoded responses
        List<Loan> loans = generateHardcodedLoans(userId);
        
        return LoanResponse.builder()
                .userId(userId)
                .totalLoans(loans.size())
                .loans(loans)
                .message("Loans retrieved successfully")
                .build();
    }

    /**
     * Generate hardcoded loan data based on userId
     */
    private List<Loan> generateHardcodedLoans(String userId) {
        List<Loan> loans = new ArrayList<>();
        
        // Generate different loan data based on userId hash
        int hashCode = userId.hashCode();
        int loanCount = Math.abs(hashCode % 3) + 1; // 1 to 3 loans
        
        long currentTime = Instant.now().toEpochMilli();
        
        for (int i = 0; i < loanCount; i++) {
            Loan loan = createSampleLoan(userId, i, currentTime);
            loans.add(loan);
        }
        
        return loans;
    }

    /**
     * Create a sample loan
     */
    private Loan createSampleLoan(String userId, int index, long currentTime) {
        String[] loanTypes = {"Home Loan", "Personal Loan", "Car Loan", "Education Loan"};
        String[] statuses = {"ACTIVE", "ACTIVE", "PAID", "PENDING"};
        
        BigDecimal amount = new BigDecimal((index + 1) * 100000);
        BigDecimal interestRate = new BigDecimal(7.5 + index);
        int tenure = (index + 1) * 12; // months
        
        // Simple EMI calculation (actual formula would be more complex)
        BigDecimal monthlyEmi = amount.multiply(BigDecimal.valueOf(0.01));
        BigDecimal outstanding = amount.multiply(BigDecimal.valueOf(0.7 - (index * 0.2)));
        
        long disbursementDate = currentTime - ((365L - (index * 100)) * 24 * 60 * 60 * 1000);
        long maturityDate = disbursementDate + (tenure * 30L * 24 * 60 * 60 * 1000);
        
        return Loan.builder()
                .loanId("LOAN-" + userId.substring(0, Math.min(8, userId.length())) + "-" + (index + 1))
                .userId(userId)
                .loanType(loanTypes[index % loanTypes.length])
                .loanAmount(amount)
                .interestRate(interestRate)
                .tenureMonths(tenure)
                .monthlyEmi(monthlyEmi)
                .outstandingBalance(outstanding)
                .status(statuses[index % statuses.length])
                .disbursementDate(disbursementDate)
                .maturityDate(maturityDate)
                .build();
    }

    /**
     * Fallback method for circuit breaker
     */
    private LoanResponse getLoansFallback(String userId, Exception e) {
        log.error("Circuit breaker activated for getUserLoans: {}", e.getMessage());
        
        return LoanResponse.builder()
                .userId(userId)
                .totalLoans(0)
                .loans(new ArrayList<>())
                .message("Loan service is temporarily unavailable. Please try again later.")
                .build();
    }
}
