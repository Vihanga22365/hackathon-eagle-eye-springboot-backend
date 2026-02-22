package com.microservices.loan.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Loan Details Upload Request
 * 
 * Contains company information and document metadata for loan applications.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanDetailsUploadRequest {
    
    private String companyName;
    private Integer companyAge;
    private String additionalComments;
    private String userId;
}
