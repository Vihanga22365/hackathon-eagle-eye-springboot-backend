package com.microservices.loan.controller;

import com.microservices.loan.model.LoanDetailsUploadRequest;
import com.microservices.loan.model.LoanResponse;
import com.microservices.loan.service.DocumentUploadService;
import com.microservices.loan.service.LoanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loan Controller
 * 
 * REST API endpoints for loan information.
 * 
 * All endpoints are protected by JWT authentication via API Gateway.
 * User information is extracted from headers set by the gateway:
 * - X-User-Id: Current user's ID
 * - X-User-Email: Current user's email
 * - X-User-Role: Current user's role
 * 
 * Endpoints:
 * - GET /api/loans/{userId} - Get loans for a specific user
 * - GET /api/loans/all-uploaded - Get all uploaded loan details for all users
 * - GET /api/loans/{userId}/uploaded/{loanId} - Get uploaded loan details by userId and loanId
 * - GET /api/loans/{userId}/latest-upload - Get latest uploaded loan details for a specific user
 * - POST /api/loans/upload - Upload loan details with documents
 * 
 * Access Control:
 * - CUSTOMER: Can only access their own loans
 * - SYSTEM_ADMIN: Can access any user's loans
 */
@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class LoanController {

    private final LoanService loanService;
    private final DocumentUploadService documentUploadService;

    /**
     * Get loans for a specific user
     * GET /api/loans/{userId}
     * 
     * Example Response:
     * {
     *   "userId": "user123",
     *   "totalLoans": 2,
     *   "loans": [
     *     {
     *       "loanId": "LOAN-user123-1",
     *       "userId": "user123",
     *       "loanType": "Home Loan",
     *       "loanAmount": 100000,
     *       "interestRate": 7.5,
     *       "tenureMonths": 12,
     *       "monthlyEmi": 1000,
     *       "outstandingBalance": 70000,
     *       "status": "ACTIVE",
     *       "disbursementDate": 1620000000000,
     *       "maturityDate": 1651536000000
     *     }
     *   ],
     *   "message": "Loans retrieved successfully"
     * }
     */
    @GetMapping("/{userId}")
    public ResponseEntity<LoanResponse> getUserLoans(
            @PathVariable String userId,
            @RequestHeader("X-User-Id") String requesterId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role) {
        
        log.info("Get loans request for user: {} by {}", userId, email);
        
        // Check authorization - users can only access their own loans, admins can access all
        if (!role.equals("SYSTEM_ADMIN") && !userId.equals(requesterId)) {
            log.warn("Unauthorized access attempt by: {} for user: {}", requesterId, userId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(LoanResponse.builder()
                            .message("You are not authorized to access this user's loans")
                            .build());
        }
        
        try {
            LoanResponse response = loanService.getUserLoans(userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching loans for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(LoanResponse.builder()
                            .message("Error fetching loans: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get current user's loans
     * GET /api/loans/my-loans
     */
    @GetMapping("/my-loans")
    public ResponseEntity<LoanResponse> getMyLoans(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Email") String email) {
        
        log.info("Get my loans request from user: {}", email);
        
        try {
            LoanResponse response = loanService.getUserLoans(userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching loans for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(LoanResponse.builder()
                            .message("Error fetching loans: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get latest uploaded loan details for a specific user
     * GET /api/loans/{userId}/latest-upload
     */
    @GetMapping("/{userId}/latest-upload")
    public ResponseEntity<Map<String, Object>> getLatestUploadedLoanDetails(
            @PathVariable String userId,
            @RequestHeader("X-User-Id") String requesterId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role) {

        log.info("Get latest uploaded loan details request for user: {} by {}", userId, email);

        if (!role.equals("SYSTEM_ADMIN") && !userId.equals(requesterId)) {
            log.warn("Unauthorized latest-upload access attempt by: {} for user: {}", requesterId, userId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "message", "You are not authorized to access this user's uploaded loan details",
                    "success", false
            ));
        }

        try {
            Map<String, Object> response = documentUploadService.getLatestLoanDetails(userId);
            if (Boolean.TRUE.equals(response.get("success"))) {
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Error fetching latest uploaded loan details for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "Error fetching latest uploaded loan details: " + e.getMessage(),
                    "success", false
            ));
        }
    }

    /**
     * Get uploaded loan details by userId and loanId
     * GET /api/loans/{userId}/uploaded/{loanId}
     */
    @GetMapping("/{userId}/uploaded/{loanId}")
    public ResponseEntity<Map<String, Object>> getUploadedLoanDetailsByUserAndLoanId(
            @PathVariable String userId,
            @PathVariable String loanId) {

        log.info("Get uploaded loan details request for user: {} loanId: {}", userId, loanId);

        try {
            Map<String, Object> response = documentUploadService.getLoanDetailsByUserIdAndLoanId(userId, loanId);
            if (Boolean.TRUE.equals(response.get("success"))) {
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Error fetching uploaded loan details for user {} and loanId {}: {}", userId, loanId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "Error fetching uploaded loan details: " + e.getMessage(),
                    "success", false
            ));
        }
    }

    /**
     * Get all uploaded loan details across all users
     * GET /api/loans/all-uploaded
     */
    @GetMapping("/all-uploaded")
    public ResponseEntity<Map<String, Object>> getAllUploadedLoanDetails() {

        log.info("Get all uploaded loan details request");

        try {
            Map<String, Object> response = documentUploadService.getAllLoanDetails();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching all uploaded loan details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "Error fetching all uploaded loan details: " + e.getMessage(),
                    "success", false
            ));
        }
    }

    /**
     * Upload loan details with supporting documents
     * POST /api/loans/upload
     * 
     * Request Parameters:
     * - companyName (String): Name of the company
     * - companyAge (Integer): Age of the company in years
     * - additionalComments (String): Additional comments
     * - documents (MultipartFile[]): 1 or more PDF documents
     * 
     * Example Response:
     * {
     *   "uploadId": "UPLOAD-1708243200000-abc12345",
     *   "userId": "user123",
     *   "companyName": "Tech Corp",
     *   "companyAge": 5,
     *   "additionalComments": "Expansion loan request",
     *   "uploadedDocuments": ["document1.pdf", "document2.pdf"],
     *   "firebasePath": "loanUploads/user123/UPLOAD-1708243200000-abc12345",
     *   "message": "Loan details and documents uploaded successfully",
     *   "success": true
     * }
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadLoanDetails(
            @RequestParam("companyName") String companyName,
            @RequestParam("companyAge") Integer companyAge,
            @RequestParam("additionalComments") String additionalComments,
            @RequestParam("documents") MultipartFile[] documents,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Email") String email) {
        
        log.info("Upload loan details request from user: {} for company: {}", email, companyName);
        
        // Validate input
        if (documents == null || documents.length == 0) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", "At least one document is required",
                "success", false
            ));
        }
        
        // Validate all files are PDFs
        List<MultipartFile> validDocuments = new ArrayList<>();
        for (MultipartFile file : documents) {
            if (!documentUploadService.isValidPdfFile(file)) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", "All documents must be PDF files. Invalid file: " +
                    (file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown"),
                "success", false
            ));
            }
            validDocuments.add(file);
        }
        
        try {
            // Create request object
            LoanDetailsUploadRequest request = LoanDetailsUploadRequest.builder()
                    .companyName(companyName)
                    .companyAge(companyAge)
                    .additionalComments(additionalComments)
                    .userId(userId)
                    .build();
            
            // Process upload
                Map<String, Object> response = documentUploadService.uploadLoanDetails(
                    request, validDocuments);
            
                if (Boolean.TRUE.equals(response.get("success"))) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
        } catch (Exception e) {
            log.error("Error uploading loan details for user {}: {}", userId, e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "Error uploading loan details: " + e.getMessage(),
                    "success", false
                ));
        }
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Loan Service is running");
    }
}
