package com.microservices.loan.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Loan Model
 * 
 * Represents loan information for a user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Loan {
    
    private String loanId;
    private String userId;
    private String loanType;
    private BigDecimal loanAmount;
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private BigDecimal monthlyEmi;
    private BigDecimal outstandingBalance;
    private String status;
    private Long disbursementDate;
    private Long maturityDate;
}
