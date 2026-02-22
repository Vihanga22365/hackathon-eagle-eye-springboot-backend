package com.microservices.loan.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Loan Response
 * 
 * Response containing user's loan information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanResponse {
    
    private String userId;
    private Integer totalLoans;
    private List<Loan> loans;
    private String message;
}
