package com.microservices.loan.service;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.microservices.loan.model.LoanDetailsUploadRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentUploadService {

    private final FirebaseDatabase firebaseDatabase;

    @Value("${document.storage.path:./documents}")
    private String documentStoragePath;

    public Map<String, Object> uploadLoanDetails(LoanDetailsUploadRequest request, List<MultipartFile> documents) {
        log.info("Uploading loan details for user: {} company: {}", request.getUserId(), request.getCompanyName());

        try {
            String uploadId = generateUploadId();
            Path storagePath = createStorageDirectory(request.getUserId(), uploadId);

            List<String> documentNames = new ArrayList<>();
            Map<String, String> documentContents = new HashMap<>();

            for (int i = 0; i < documents.size(); i++) {
                MultipartFile file = documents.get(i);
                String documentKey = "document" + (i + 1);

                String savedFileName = saveDocumentLocally(file, storagePath, documentKey);
                documentNames.add(savedFileName);

                String content = extractPdfContent(file);
                documentContents.put(documentKey, content);
            }

            String firebasePath = saveToFirebase(uploadId, request, documentContents);

            Map<String, Object> response = new HashMap<>();
            response.put("uploadId", uploadId);
            response.put("userId", request.getUserId());
            response.put("companyName", request.getCompanyName());
            response.put("companyAge", request.getCompanyAge());
            response.put("additionalComments", request.getAdditionalComments());
            response.put("uploadedDocuments", documentNames);
            response.put("firebasePath", firebasePath);
            response.put("message", "Loan details and documents uploaded successfully");
            response.put("success", true);
            return response;
        } catch (Exception e) {
            log.error("Error uploading loan details", e);
            Map<String, Object> response = new HashMap<>();
            response.put("userId", request.getUserId());
            response.put("message", "Failed to upload loan details: " + e.getMessage());
            response.put("success", false);
            return response;
        }
    }

    public Map<String, Object> getLatestLoanDetails(String userId) {
        try {
            DataSnapshot snapshot = fetchUserUploadsSnapshot(userId);
            if (snapshot == null || !snapshot.exists()) {
                return Map.of(
                        "userId", userId,
                        "message", "No uploaded loan details found for this user",
                        "success", false
                );
            }

            Map<String, Object> latestUpload = null;
            long latestTimestamp = Long.MIN_VALUE;

            for (DataSnapshot uploadSnapshot : snapshot.getChildren()) {
                Map<String, Object> uploadData = snapshotToMap(uploadSnapshot, userId);
                if (uploadData == null) {
                    continue;
                }

                long uploadTimestamp = asLong(uploadData.get("uploadTimestamp"));
                if (uploadTimestamp > latestTimestamp) {
                    latestTimestamp = uploadTimestamp;
                    latestUpload = uploadData;
                }
            }

            if (latestUpload == null) {
                return Map.of(
                        "userId", userId,
                        "message", "No valid uploaded loan details found for this user",
                        "success", false
                );
            }

            Map<String, Object> response = new HashMap<>(latestUpload);
            response.put("message", "Latest loan details fetched successfully");
            response.put("success", true);
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while fetching latest loan details", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch latest loan details from Firebase", e);
        }
    }

    public Map<String, Object> getLoanDetailsByUserIdAndLoanId(String userId, String loanId) {
        try {
            DataSnapshot snapshot = fetchUserUploadsSnapshot(userId);
            if (snapshot == null || !snapshot.exists()) {
                return Map.of(
                        "userId", userId,
                        "loanId", loanId,
                        "message", "No uploaded loan details found for this user",
                        "success", false
                );
            }

            DataSnapshot loanSnapshot = snapshot.child(loanId);
            if (!loanSnapshot.exists()) {
                return Map.of(
                        "userId", userId,
                        "loanId", loanId,
                        "message", "Loan details not found for the given loanId",
                        "success", false
                );
            }

            Map<String, Object> loanData = snapshotToMap(loanSnapshot, userId);
            if (loanData == null) {
                return Map.of(
                        "userId", userId,
                        "loanId", loanId,
                        "message", "Invalid loan details format in database",
                        "success", false
                );
            }

            loanData.put("message", "Loan details fetched successfully");
            loanData.put("success", true);
            return loanData;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while fetching loan details", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch loan details from Firebase", e);
        }
    }

    public Map<String, Object> getAllLoanDetails() {
        try {
            DataSnapshot snapshot = fetchAllUploadsSnapshot();
            if (snapshot == null || !snapshot.exists()) {
                return Map.of(
                        "total", 0,
                        "users", 0,
                        "loanDetails", Collections.emptyList(),
                        "message", "No uploaded loan details found",
                        "success", true
                );
            }

            List<Map<String, Object>> allDetails = new ArrayList<>();
            int usersCount = 0;

            for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                String userId = userSnapshot.getKey();
                if (userId == null) {
                    continue;
                }
                usersCount++;

                for (DataSnapshot uploadSnapshot : userSnapshot.getChildren()) {
                    Map<String, Object> uploadData = snapshotToMap(uploadSnapshot, userId);
                    if (uploadData != null) {
                        allDetails.add(uploadData);
                    }
                }
            }

            allDetails.sort((left, right) -> Long.compare(
                    asLong(right.get("uploadTimestamp")),
                    asLong(left.get("uploadTimestamp"))
            ));

            Map<String, Object> response = new HashMap<>();
            response.put("total", allDetails.size());
            response.put("users", usersCount);
            response.put("loanDetails", allDetails);
            response.put("message", "All loan details fetched successfully");
            response.put("success", true);
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while fetching all loan details", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch all loan details from Firebase", e);
        }
    }

    public boolean isValidPdfFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            return false;
        }

        boolean isPdf = filename.toLowerCase().endsWith(".pdf");
        String contentType = file.getContentType();
        boolean isValidContentType = contentType != null && contentType.equals("application/pdf");

        return isPdf && isValidContentType;
    }

    private String generateUploadId() {
        return "UPLOAD-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private Path createStorageDirectory(String userId, String uploadId) throws IOException {
        Path path = Paths.get(documentStoragePath, userId, uploadId);
        Files.createDirectories(path);
        return path;
    }

    private String saveDocumentLocally(MultipartFile file, Path storagePath, String documentKey) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".pdf";

        String fileName = documentKey + extension;
        Path filePath = storagePath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return fileName;
    }

    private String extractPdfContent(MultipartFile file) {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String content = stripper.getText(document);
            int maxLength = 10000;
            if (content.length() > maxLength) {
                return content.substring(0, maxLength) + "...[truncated]";
            }
            return content;
        } catch (IOException e) {
            return "Error extracting PDF content: " + e.getMessage();
        }
    }

    private String saveToFirebase(String uploadId, LoanDetailsUploadRequest request, Map<String, String> documentContents) {
        try {
            String firebasePath = "loanUploads/" + request.getUserId() + "/" + uploadId;
            DatabaseReference ref = firebaseDatabase.getReference(firebasePath);

            Map<String, Object> uploadData = new HashMap<>();
            uploadData.put("uploadId", uploadId);
            uploadData.put("companyName", request.getCompanyName());
            uploadData.put("companyAge", request.getCompanyAge());
            uploadData.put("additionalComments", request.getAdditionalComments());
            uploadData.put("uploadTimestamp", Instant.now().toEpochMilli());
            documentContents.forEach(uploadData::put);

            CompletableFuture<Void> future = new CompletableFuture<>();
            ref.setValueAsync(uploadData)
                    .addListener(() -> future.complete(null), Runnable::run);

            return firebasePath;
        } catch (Exception e) {
            throw new RuntimeException("Firebase save failed", e);
        }
    }

    private DataSnapshot fetchUserUploadsSnapshot(String userId) throws InterruptedException {
        DatabaseReference userUploadsRef = firebaseDatabase.getReference("loanUploads").child(userId);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<DataSnapshot> snapshotRef = new AtomicReference<>();
        AtomicReference<DatabaseError> errorRef = new AtomicReference<>();

        userUploadsRef.addListenerForSingleValueEvent(new ValueEventListener() {
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
            throw new RuntimeException("Timed out while fetching loan details from Firebase");
        }
        if (errorRef.get() != null) {
            throw new RuntimeException("Firebase read failed", errorRef.get().toException());
        }

        return snapshotRef.get();
    }

    private DataSnapshot fetchAllUploadsSnapshot() throws InterruptedException {
        DatabaseReference allUploadsRef = firebaseDatabase.getReference("loanUploads");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<DataSnapshot> snapshotRef = new AtomicReference<>();
        AtomicReference<DatabaseError> errorRef = new AtomicReference<>();

        allUploadsRef.addListenerForSingleValueEvent(new ValueEventListener() {
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
            throw new RuntimeException("Timed out while fetching all loan details from Firebase");
        }
        if (errorRef.get() != null) {
            throw new RuntimeException("Firebase read failed", errorRef.get().toException());
        }

        return snapshotRef.get();
    }

    private Map<String, Object> snapshotToMap(DataSnapshot snapshot, String userId) {
        Object value = snapshot.getValue();
        if (!(value instanceof Map<?, ?> valueMapRaw)) {
            return null;
        }

        Map<String, Object> uploadData = new HashMap<>();
        for (Map.Entry<?, ?> entry : valueMapRaw.entrySet()) {
            if (entry.getKey() != null) {
                uploadData.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }

        String uploadId = snapshot.getKey();
        uploadData.putIfAbsent("uploadId", uploadId);
        uploadData.putIfAbsent("loanId", uploadId);
        uploadData.putIfAbsent("userId", userId);
        return uploadData;
    }

    private long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return Long.MIN_VALUE;
            }
        }
        return Long.MIN_VALUE;
    }
}
