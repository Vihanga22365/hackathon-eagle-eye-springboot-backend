package com.microservices.auth.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Firebase Configuration
 * 
 * Initializes Firebase Admin SDK with service account credentials.
 * This enables server-side Firebase operations like user management,
 * token verification, and database access.
 * 
 * Before running the application:
 * 1. Create a Firebase project at https://console.firebase.google.com
 * 2. Generate a service account key (Project Settings > Service Accounts)
 * 3. Download the JSON file and place it in src/main/resources
 * 4. Update the firebase.credentials.path in application.yml
 */
@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.credentials.path:classpath:firebase-service-account.json}")
    private Resource credentialsResource;

    @Value("${firebase.credentials.project_id:${FIREBASE_PROJECT_ID:}}")
    private String projectId;

    @Value("${firebase.credentials.private_key_id:${FIREBASE_PRIVATE_KEY_ID:}}")
    private String privateKeyId;

    @Value("${firebase.credentials.private_key:${FIREBASE_PRIVATE_KEY:}}")
    private String privateKey;

    @Value("${firebase.credentials.client_email:${FIREBASE_CLIENT_EMAIL:}}")
    private String clientEmail;

    @Value("${firebase.database.url}")
    private String databaseUrl;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        log.info("Initializing Firebase Admin SDK");
        
        try {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(resolveCredentials())
                    .setDatabaseUrl(databaseUrl)
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp app = FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK initialized successfully");
                return app;
            } else {
                return FirebaseApp.getInstance();
            }
        } catch (Exception e) {
            log.error("Failed to initialize Firebase Admin SDK", e);
            throw e;
        }
    }

    private GoogleCredentials resolveCredentials() throws IOException {
        if (StringUtils.hasText(projectId)
                && StringUtils.hasText(clientEmail)
                && StringUtils.hasText(privateKey)) {
            log.info("Using Firebase credentials from environment properties");

            String trimmedPrivateKey = privateKey.trim();
            if (trimmedPrivateKey.startsWith("{")) {
                log.info("FIREBASE_PRIVATE_KEY contains JSON content; loading credentials from JSON");
                try (InputStream jsonStream = new ByteArrayInputStream(trimmedPrivateKey.getBytes(StandardCharsets.UTF_8))) {
                    return GoogleCredentials.fromStream(jsonStream);
                }
            }

            String normalizedPrivateKey = privateKey.replace("\\n", "\n");
            return ServiceAccountCredentials.fromPkcs8(
                    projectId,
                    clientEmail,
                    normalizedPrivateKey,
                    privateKeyId,
                    null,
                    null);
        }

        log.info("Using Firebase credentials from file resource");
        try (InputStream serviceAccount = credentialsResource.getInputStream()) {
            return GoogleCredentials.fromStream(serviceAccount);
        }
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    @Bean
    public FirebaseDatabase firebaseDatabase(FirebaseApp firebaseApp) {
        return FirebaseDatabase.getInstance(firebaseApp);
    }
}
