package com.microservices.loan.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.microservices.loan.model.AiAgentsFeedback;
import com.microservices.loan.model.AiAgentsFeedbackRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service for managing AI Agents Feedback stored in Firebase
 * under the `aiAgentsFeedback` path.
 *
 * Firebase structure:
 *   aiAgentsFeedback/{feedbackId}
 *     id, loggedUserId, loanId, loanUserId,
 *     timestamp, dateTime, summary{...}, agentResponseSummary{...}
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiAgentsFeedbackService {

    private static final String FB_PATH = "aiAgentsFeedback";
    private static final DateTimeFormatter DT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private final FirebaseDatabase firebaseDatabase;
    private final ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    /**
     * Persist a new AI agents feedback record in Firebase.
     *
     * @param request      payload from the caller
     * @param loggedUserId ID of the authenticated user making the request
     * @return the persisted {@link AiAgentsFeedback}
     */
    public AiAgentsFeedback createFeedback(AiAgentsFeedbackRequest request, String loggedUserId) {
        log.info("Creating AI agents feedback for loanId={} by loggedUserId={}", request.getLoanId(), loggedUserId);

        String id = generateFeedbackId();
        long timestamp = Instant.now().toEpochMilli();
        String dateTime = DT_FORMATTER.format(Instant.ofEpochMilli(timestamp));

        AiAgentsFeedback feedback = AiAgentsFeedback.builder()
                .id(id)
                .loggedUserId(loggedUserId)
                .loanId(request.getLoanId())
                .loanUserId(request.getLoanUserId())
                .timestamp(timestamp)
                .dateTime(dateTime)
                .summary(request.getSummary())
                .agentResponseSummary(request.getAgentResponseSummary())
                .build();

        saveToFirebase(id, feedback);
        log.info("AI agents feedback saved with id={}", id);
        return feedback;
    }

    // -------------------------------------------------------------------------
    // Read — by feedbackId
    // -------------------------------------------------------------------------

    /**
     * Retrieve a single feedback record by its unique ID.
     *
     * @return the {@link AiAgentsFeedback} or {@code null} if not found
     */
    public AiAgentsFeedback getFeedbackById(String feedbackId) {
        log.info("Fetching AI agents feedback by id={}", feedbackId);
        DataSnapshot snapshot = fetchSnapshot(FB_PATH + "/" + feedbackId);
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }
        AiAgentsFeedback feedback = snapshotToFeedback(snapshot);
        return enrichFeedbackWithUserNames(feedback);
    }

    // -------------------------------------------------------------------------
    // Read — all
    // -------------------------------------------------------------------------

    /**
     * Retrieve every feedback record ordered newest-first.
     */
    public List<AiAgentsFeedback> getAllFeedback() {
        log.info("Fetching all AI agents feedback records");
        DataSnapshot snapshot = fetchSnapshot(FB_PATH);

        if (snapshot == null || !snapshot.exists()) {
            return Collections.emptyList();
        }

        List<AiAgentsFeedback> results = new ArrayList<>();
        for (DataSnapshot child : snapshot.getChildren()) {
            AiAgentsFeedback fb = enrichFeedbackWithUserNames(snapshotToFeedback(child));
            if (fb != null) {
                results.add(fb);
            }
        }

        results.sort((a, b) -> Long.compare(
                b.getTimestamp() != null ? b.getTimestamp() : 0L,
                a.getTimestamp() != null ? a.getTimestamp() : 0L));

        log.info("Retrieved {} AI agents feedback records", results.size());
        return results;
    }

    // -------------------------------------------------------------------------
    // Read — by loanId
    // -------------------------------------------------------------------------

    /**
     * Retrieve all feedback records that belong to a specific loan.
     */
    public List<AiAgentsFeedback> getFeedbackByLoanId(String loanId) {
        log.info("Fetching AI agents feedback for loanId={}", loanId);
        List<AiAgentsFeedback> all = getAllFeedback();
        List<AiAgentsFeedback> results = new ArrayList<>();
        for (AiAgentsFeedback fb : all) {
            if (loanId.equals(fb.getLoanId())) {
                results.add(fb);
            }
        }
        log.info("Found {} feedback records for loanId={}", results.size(), loanId);
        return results;
    }

    // -------------------------------------------------------------------------
    // Read — by loanUserId
    // -------------------------------------------------------------------------

    /**
     * Retrieve all feedback records for the owner of the loan.
     */
    public List<AiAgentsFeedback> getFeedbackByLoanUserId(String loanUserId) {
        log.info("Fetching AI agents feedback for loanUserId={}", loanUserId);
        List<AiAgentsFeedback> all = getAllFeedback();
        List<AiAgentsFeedback> results = new ArrayList<>();
        for (AiAgentsFeedback fb : all) {
            if (loanUserId.equals(fb.getLoanUserId())) {
                results.add(fb);
            }
        }
        log.info("Found {} feedback records for loanUserId={}", results.size(), loanUserId);
        return results;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String generateFeedbackId() {
        return "FEEDBACK-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void saveToFirebase(String id, AiAgentsFeedback feedback) {
        try {
            DatabaseReference ref = firebaseDatabase.getReference(FB_PATH).child(id);

            // Convert POJO → generic Map so Firebase serialises nested objects correctly
            Map<String, Object> data = objectMapper.convertValue(feedback,
                    new TypeReference<Map<String, Object>>() {});

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<DatabaseError> errorRef = new AtomicReference<>();

            ref.setValue(data, (error, dbRef) -> {
                if (error != null) {
                    errorRef.set(error);
                }
                latch.countDown();
            });

            boolean completed = latch.await(15, TimeUnit.SECONDS);
            if (!completed) {
                throw new RuntimeException("Timed out while saving AI agents feedback to Firebase");
            }
            if (errorRef.get() != null) {
                throw new RuntimeException("Firebase write failed: " + errorRef.get().getMessage());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while saving AI agents feedback", e);
        }
    }

    private DataSnapshot fetchSnapshot(String path) {
        try {
            DatabaseReference ref = firebaseDatabase.getReference(path);
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<DataSnapshot> snapshotRef = new AtomicReference<>();
            AtomicReference<DatabaseError> errorRef = new AtomicReference<>();

            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    snapshotRef.set(snapshot);
                    latch.countDown();
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    errorRef.set(error);
                    latch.countDown();
                }
            });

            boolean completed = latch.await(15, TimeUnit.SECONDS);
            if (!completed) {
                throw new RuntimeException("Timed out while reading from Firebase path: " + path);
            }
            if (errorRef.get() != null) {
                throw new RuntimeException("Firebase read failed: " + errorRef.get().getMessage());
            }
            return snapshotRef.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while reading from Firebase", e);
        }
    }

    @SuppressWarnings("unchecked")
    private AiAgentsFeedback snapshotToFeedback(DataSnapshot snapshot) {
        try {
            Object raw = snapshot.getValue();
            if (!(raw instanceof Map)) {
                return null;
            }
            // Re-inflate via Jackson for type safety
            Map<String, Object> map = new HashMap<>((Map<String, Object>) raw);
            return objectMapper.convertValue(map, AiAgentsFeedback.class);
        } catch (Exception e) {
            log.warn("Could not deserialise feedback snapshot key={}: {}", snapshot.getKey(), e.getMessage());
            return null;
        }
    }

    private AiAgentsFeedback enrichFeedbackWithUserNames(AiAgentsFeedback feedback) {
        if (feedback == null) {
            return null;
        }

        String loggedUserName = resolveUserFullName(feedback.getLoggedUserId());
        String loanUserName = resolveUserFullName(feedback.getLoanUserId());

        feedback.setLoggedUserName(loggedUserName);
        feedback.setLoanUserName(loanUserName);
        return feedback;
    }

    private String resolveUserFullName(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }

        try {
            DataSnapshot snapshot = fetchSnapshot("users/" + userId + "/fullName");
            if (snapshot == null || !snapshot.exists()) {
                return null;
            }
            Object value = snapshot.getValue();
            return value != null ? String.valueOf(value) : null;
        } catch (Exception e) {
            log.warn("Failed to resolve fullName for userId={}: {}", userId, e.getMessage());
            return null;
        }
    }
}
