package com.microservices.auth.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.microservices.auth.dto.AuthResponse;
import com.microservices.auth.dto.LoginRequest;
import com.microservices.auth.dto.RegisterRequest;
import com.microservices.auth.model.UserRole;
import com.microservices.auth.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication Service
 * 
 * Handles user registration, login, and token generation using Firebase Authentication.
 * 
 * Note: Firebase Authentication with email/password requires Firebase SDK setup.
 * For production, you should:
 * 1. Enable Email/Password authentication in Firebase Console
 * 2. Implement proper password validation
 * 3. Add email verification flow
 * 4. Store additional user data (like role) in Firebase Realtime Database or Firestore
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final FirebaseAuth firebaseAuth;
    private final FirebaseDatabase firebaseDatabase;
    private final JwtUtil jwtUtil;

    /**
     * Register a new user with Firebase Authentication
     */
    public AuthResponse register(RegisterRequest request) {
        try {
            log.info("Registering new user: {}", request.getEmail());

            // Create user in Firebase
            UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                    .setEmail(request.getEmail())
                    .setPassword(request.getPassword())
                    .setDisplayName(request.getFullName())
                    .setEmailVerified(false);

            UserRecord userRecord = firebaseAuth.createUser(createRequest);

            // Set custom claims for role-based authorization
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", request.getRole().name());
            firebaseAuth.setCustomUserClaims(userRecord.getUid(), claims);

            // Save user profile to Realtime Database
            DatabaseReference usersRef = firebaseDatabase.getReference("users");
            Map<String, Object> userProfile = new HashMap<>();
            userProfile.put("userId", userRecord.getUid());
            userProfile.put("email", request.getEmail());
            userProfile.put("fullName", request.getFullName());
            userProfile.put("role", request.getRole().name());
            userProfile.put("phoneNumber", "");
            userProfile.put("address", "");
            userProfile.put("createdAt", System.currentTimeMillis());
            
            // Wait for async operation to complete
            try {
                usersRef.child(userRecord.getUid()).setValueAsync(userProfile).get();
                log.info("User profile saved to database");
            } catch (Exception e) {
                log.warn("Failed to save user profile to database: {}", e.getMessage());
                // Continue anyway - user is created in Auth
            }

            log.info("User registered successfully: {}", userRecord.getUid());

            // Generate JWT token
            String token = jwtUtil.generateToken(
                    userRecord.getUid(),
                    request.getEmail(),
                    request.getRole()
            );

            return AuthResponse.builder()
                    .token(token)
                    .userId(userRecord.getUid())
                    .email(request.getEmail())
                    .fullName(request.getFullName())
                    .role(request.getRole())
                    .message("Registration successful")
                    .build();

        } catch (FirebaseAuthException e) {
            log.error("Firebase registration error: {}", e.getMessage());
            throw new RuntimeException("Registration failed: " + e.getMessage());
        }
    }

    /**
     * Login user with Firebase Authentication
     * 
     * Note: Firebase Admin SDK doesn't directly support email/password sign-in.
     * In production, you should:
     * 1. Use Firebase Client SDK on frontend for authentication
     * 2. Send Firebase ID token to backend
     * 3. Verify token and generate your own JWT
     * 
     * This implementation is simplified for demonstration.
     */
    public AuthResponse login(LoginRequest request) {
        try {
            log.info("User login attempt: {}", request.getEmail());

            // Retrieve user by email
            UserRecord userRecord = firebaseAuth.getUserByEmail(request.getEmail());

            // In production, verify password using Firebase Client SDK or custom logic
            // This is a simplified version
            
            // Get custom claims (role)
            UserRole role = UserRole.CUSTOMER;
            if (userRecord.getCustomClaims() != null && 
                userRecord.getCustomClaims().containsKey("role")) {
                role = UserRole.valueOf(
                    userRecord.getCustomClaims().get("role").toString()
                );
            }

            // Generate JWT token
            String token = jwtUtil.generateToken(
                    userRecord.getUid(),
                    userRecord.getEmail(),
                    role
            );

            log.info("User logged in successfully: {}", userRecord.getUid());

            return AuthResponse.builder()
                    .token(token)
                    .userId(userRecord.getUid())
                    .email(userRecord.getEmail())
                    .fullName(userRecord.getDisplayName())
                    .role(role)
                    .message("Login successful")
                    .build();

        } catch (FirebaseAuthException e) {
            log.error("Firebase login error: {}", e.getMessage());
            throw new RuntimeException("Login failed: " + e.getMessage());
        }
    }

    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }

    /**
     * Extract user ID from token
     */
    public String getUserIdFromToken(String token) {
        return jwtUtil.extractUserId(token);
    }

    /**
     * Extract role from token
     */
    public UserRole getRoleFromToken(String token) {
        return jwtUtil.extractRole(token);
    }

    /**
     * Refresh an expired (or soon-to-expire) JWT token.
     *
     * Extracts userId/email/role from the old token without checking expiry,
     * re-verifies the user still exists in Firebase, then issues a fresh token.
     */
    public AuthResponse refreshToken(String expiredToken) {
        try {
            log.info("Refreshing token");

            // Extract claims ignoring expiry
            Claims claims = jwtUtil.extractClaimsIgnoreExpiry(expiredToken);
            String userId = claims.get("userId", String.class);
            String role   = claims.get("role",   String.class);

            if (userId == null || role == null) {
                throw new RuntimeException("Token missing required claims");
            }

            // Re-fetch user from Firebase to confirm the account still exists
            UserRecord userRecord = firebaseAuth.getUser(userId);

            // Honour any role update that may have happened since the original token was issued
            UserRole userRole = UserRole.valueOf(role);
            if (userRecord.getCustomClaims() != null
                    && userRecord.getCustomClaims().containsKey("role")) {
                userRole = UserRole.valueOf(
                        userRecord.getCustomClaims().get("role").toString());
            }

            String newToken = jwtUtil.generateToken(
                    userRecord.getUid(),
                    userRecord.getEmail(),
                    userRole);

            log.info("Token refreshed successfully for user: {}", userRecord.getUid());

            return AuthResponse.builder()
                    .token(newToken)
                    .userId(userRecord.getUid())
                    .email(userRecord.getEmail())
                    .fullName(userRecord.getDisplayName())
                    .role(userRole)
                    .message("Token refreshed successfully")
                    .build();

        } catch (FirebaseAuthException e) {
            log.error("Firebase error during token refresh: {}", e.getMessage());
            throw new RuntimeException("Token refresh failed: user not found");
        } catch (Exception e) {
            log.error("Token refresh error: {}", e.getMessage());
            throw new RuntimeException("Token refresh failed: " + e.getMessage());
        }
    }
}