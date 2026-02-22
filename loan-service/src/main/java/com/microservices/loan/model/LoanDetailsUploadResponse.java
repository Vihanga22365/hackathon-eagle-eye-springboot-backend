package com.microservices.loan.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Loan Details Upload Response
 * 
 * Response after uploading loan details and documents.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanDetailsUploadResponse {
    
    private String uploadId;
    private String userId;
    private String companyName;
    private Integer companyAge;
    private String additionalComments;
    private List<String> uploadedDocuments;
    private String firebasePath;
    private String message;
    private boolean success;
}
