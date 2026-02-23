package com.microservices.loan.controller;

import com.microservices.loan.model.AiAgentsFeedback;
import com.microservices.loan.model.AiAgentsFeedbackRequest;
import com.microservices.loan.service.AiAgentsFeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * AI Agents Feedback Controller
 *
 * Provides CRUD endpoints for the `aiAgentsFeedback` Firebase collection.
 *
 * Endpoints:
 *   POST   /api/ai-agents-feedback              – Create a feedback record
 *   GET    /api/ai-agents-feedback              – Get all feedback records
 *   GET    /api/ai-agents-feedback/loan/{loanId}          – Get by loan ID
 *   GET    /api/ai-agents-feedback/loan-user/{loanUserId} – Get by loan user ID
 *
 * All endpoints require JWT authentication via the API Gateway.
 * The logged-in user's ID is taken from the X-User-Id header.
 */
@RestController
@RequestMapping("/api/ai-agents-feedback")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AiAgentsFeedbackController {

    private final AiAgentsFeedbackService aiAgentsFeedbackService;

    // -----------------------------------------------------------------------
    // GET /api/ai-agents-feedback/{feedbackId}
    // -----------------------------------------------------------------------

    /**
     * Retrieve a single AI agents feedback record by its unique feedback ID.
     */
    @GetMapping("/{feedbackId}")
    public ResponseEntity<Map<String, Object>> getFeedbackById(
            @PathVariable String feedbackId,
            @RequestHeader("X-User-Id") String requesterId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role) {

        log.info("Get AI agents feedback by id={} by userId={}", feedbackId, requesterId);

        try {
            AiAgentsFeedback feedback = aiAgentsFeedbackService.getFeedbackById(feedbackId);
            if (feedback == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "feedbackId", feedbackId,
                        "message", "Feedback record not found",
                        "success", false));
            }

            // Non-admins may only view feedback tied to their own loan
            if (!"SYSTEM_ADMIN".equals(role) && !requesterId.equals(feedback.getLoanUserId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "message", "You are not authorized to access this feedback record",
                        "success", false));
            }

            return ResponseEntity.ok(Map.of(
                    "data", feedback,
                    "message", "AI agents feedback retrieved successfully",
                    "success", true));
        } catch (Exception e) {
            log.error("Error retrieving AI agents feedback id={}: {}", feedbackId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "Error retrieving AI agents feedback: " + e.getMessage(),
                    "success", false));
        }
    }

    // -----------------------------------------------------------------------
    // POST /api/ai-agents-feedback
    // -----------------------------------------------------------------------

    /**
     * Create a new AI agents feedback record.
     *
     * The logged-in user ID is automatically read from the X-User-Id header
     * (set by the API Gateway). All other fields must be provided in the body.
     *
     * Request body example:
     * {
     *   "loanId": "UPLOAD-xxx",
     *   "loanUserId": "user456",
     *   "summary": {
     *     "riskLevel": 65,
     *     "riskReason": "- bullet1\n- bullet2",
     *     "loanTypeDecision": "Non Secured",
     *     "loanTypeReason": "- bullet1\n- bullet2",
     *     "recommendedRate": "5%-8%",
     *     "rateReason": "- bullet1\n- bullet2",
     *     "requestedLoanAmount": "50000",
     *     "recommendedLoanAmount": "40000-60000",
     *     "reasonForLoanAmount": "- bullet1\n- bullet2"
     *   },
     *   "agentResponseSummary": {
     *     "agentTasks": [{ "agent": "...", "task": "..." }, ...],
     *     "decisions": { "verificationAnalyzerAgent": "...", ... }
     *   }
     * }
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createFeedback(
            @RequestBody AiAgentsFeedbackRequest request,
            @RequestHeader("X-User-Id") String loggedUserId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role) {

        log.info("Create AI agents feedback request by userId={} email={}", loggedUserId, email);

        // Basic validation
        if (request.getLoanId() == null || request.getLoanId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "loanId is required",
                    "success", false));
        }
        if (request.getLoanUserId() == null || request.getLoanUserId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "loanUserId is required",
                    "success", false));
        }
        if (request.getSummary() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "summary is required",
                    "success", false));
        }
        if (request.getAgentResponseSummary() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "agentResponseSummary is required",
                    "success", false));
        }
        if (request.getAgentResponseSummary().getAgentTasks() == null
                || request.getAgentResponseSummary().getAgentTasks().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "agentResponseSummary.agentTasks must contain at least one entry",
                    "success", false));
        }

        try {
            AiAgentsFeedback feedback = aiAgentsFeedbackService.createFeedback(request, loggedUserId);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "data", feedback,
                    "message", "AI agents feedback created successfully",
                    "success", true));
        } catch (Exception e) {
            log.error("Error creating AI agents feedback: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "Error creating AI agents feedback: " + e.getMessage(),
                    "success", false));
        }
    }

    // -----------------------------------------------------------------------
    // GET /api/ai-agents-feedback
    // -----------------------------------------------------------------------

    /**
     * Retrieve all AI agents feedback records, ordered newest-first.
     * Accessible by SYSTEM_ADMIN only.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllFeedback(
            @RequestHeader("X-User-Id") String requesterId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role) {

        log.info("Get all AI agents feedback request by userId={}", requesterId);

        if (!"SYSTEM_ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "message", "Only SYSTEM_ADMIN can retrieve all feedback records",
                    "success", false));
        }

        try {
            List<AiAgentsFeedback> feedbackList = aiAgentsFeedbackService.getAllFeedback();
            return ResponseEntity.ok(Map.of(
                    "total", feedbackList.size(),
                    "data", feedbackList,
                    "message", "AI agents feedback records retrieved successfully",
                    "success", true));
        } catch (Exception e) {
            log.error("Error retrieving all AI agents feedback: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "Error retrieving AI agents feedback: " + e.getMessage(),
                    "success", false));
        }
    }

    // -----------------------------------------------------------------------
    // GET /api/ai-agents-feedback/loan/{loanId}
    // -----------------------------------------------------------------------

    /**
     * Retrieve all feedback records for a specific loan ID.
     */
    @GetMapping("/loan/{loanId}")
    public ResponseEntity<Map<String, Object>> getFeedbackByLoanId(
            @PathVariable String loanId,
            @RequestHeader("X-User-Id") String requesterId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role) {

        log.info("Get AI agents feedback for loanId={} by userId={}", loanId, requesterId);

        try {
            List<AiAgentsFeedback> feedbackList = aiAgentsFeedbackService.getFeedbackByLoanId(loanId);
            return ResponseEntity.ok(Map.of(
                    "loanId", loanId,
                    "total", feedbackList.size(),
                    "data", feedbackList,
                    "message", feedbackList.isEmpty()
                            ? "No feedback records found for the given loanId"
                            : "AI agents feedback records retrieved successfully",
                    "success", true));
        } catch (Exception e) {
            log.error("Error retrieving AI agents feedback for loanId={}: {}", loanId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "Error retrieving AI agents feedback: " + e.getMessage(),
                    "success", false));
        }
    }

    // -----------------------------------------------------------------------
    // GET /api/ai-agents-feedback/loan-user/{loanUserId}
    // -----------------------------------------------------------------------

    /**
     * Retrieve all feedback records for a specific loan owner (loan user ID).
     * CUSTOMER users can only query their own ID; SYSTEM_ADMIN can query any.
     */
    @GetMapping("/loan-user/{loanUserId}")
    public ResponseEntity<Map<String, Object>> getFeedbackByLoanUserId(
            @PathVariable String loanUserId,
            @RequestHeader("X-User-Id") String requesterId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role) {

        log.info("Get AI agents feedback for loanUserId={} by userId={}", loanUserId, requesterId);

        // Access control: non-admins can only view their own feedback
        if (!"SYSTEM_ADMIN".equals(role) && !loanUserId.equals(requesterId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "message", "You are not authorized to access feedback for this user",
                    "success", false));
        }

        try {
            List<AiAgentsFeedback> feedbackList = aiAgentsFeedbackService.getFeedbackByLoanUserId(loanUserId);
            return ResponseEntity.ok(Map.of(
                    "loanUserId", loanUserId,
                    "total", feedbackList.size(),
                    "data", feedbackList,
                    "message", feedbackList.isEmpty()
                            ? "No feedback records found for the given loanUserId"
                            : "AI agents feedback records retrieved successfully",
                    "success", true));
        } catch (Exception e) {
            log.error("Error retrieving AI agents feedback for loanUserId={}: {}", loanUserId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "Error retrieving AI agents feedback: " + e.getMessage(),
                    "success", false));
        }
    }
}
