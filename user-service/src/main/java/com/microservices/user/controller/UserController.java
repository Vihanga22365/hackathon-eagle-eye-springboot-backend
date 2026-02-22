package com.microservices.user.controller;

import com.microservices.user.dto.UpdateProfileRequest;
import com.microservices.user.model.UserProfile;
import com.microservices.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * User Controller
 * 
 * REST API endpoints for user profile management.
 * 
 * All endpoints are protected by JWT authentication via API Gateway.
 * User information is extracted from headers set by the gateway:
 * - X-User-Id: Current user's ID
 * - X-User-Email: Current user's email
 * - X-User-Role: Current user's role
 * 
 * Endpoints:
 * - GET /api/users/profile - Get current user's profile
 * - PUT /api/users/profile - Update current user's profile
 * - GET /api/users/{userId} - Get specific user's profile (admin or own)
 * - GET /api/users - Get all users (admin only)
 * - POST /api/users - Create user profile
 * - DELETE /api/users/{userId} - Delete user profile (admin only)
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    /**
     * Get current user's profile
     * GET /api/users/profile
     */
    @GetMapping("/profile")
    public ResponseEntity<UserProfile> getCurrentUserProfile(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role) {
        
        log.info("Get profile request from user: {}", email);
        
        try {
            UserProfile profile = userService.getUserProfile(userId);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            log.error("Error fetching user profile: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Update current user's profile
     * PUT /api/users/profile
     */
    @PutMapping("/profile")
    public ResponseEntity<UserProfile> updateCurrentUserProfile(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody UpdateProfileRequest request) {
        
        log.info("Update profile request from user: {}", email);
        
        try {
            UserProfile updatedProfile = userService.updateUserProfile(userId, request);
            return ResponseEntity.ok(updatedProfile);
        } catch (Exception e) {
            log.error("Error updating user profile: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Get user profile by ID
     * GET /api/users/{userId}
     * Access: Admin can access all, users can only access their own
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserProfile> getUserProfile(
            @PathVariable String userId,
            @RequestHeader("X-User-Id") String requesterId,
            @RequestHeader("X-User-Role") String role) {
        
        log.info("Get user profile request: {} by {}", userId, requesterId);
        
        // Check authorization
        if (!role.equals("SYSTEM_ADMIN") && !userId.equals(requesterId)) {
            log.warn("Unauthorized access attempt by: {}", requesterId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        try {
            UserProfile profile = userService.getUserProfile(userId);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            log.error("Error fetching user profile: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Get all users (admin only)
     * GET /api/users
     */
    @GetMapping
    public ResponseEntity<List<UserProfile>> getAllUsers(
            @RequestHeader("X-User-Role") String role) {
        
        log.info("Get all users request");
        
        // Check if admin
        if (!role.equals("SYSTEM_ADMIN")) {
            log.warn("Unauthorized access attempt - not admin");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        try {
            List<UserProfile> users = userService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("Error fetching all users: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create user profile
     * POST /api/users
     */
    @PostMapping
    public ResponseEntity<UserProfile> createUserProfile(
            @RequestBody UserProfile userProfile) {
        
        log.info("Create user profile request: {}", userProfile.getEmail());
        
        try {
            UserProfile createdProfile = userService.createUserProfile(userProfile);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdProfile);
        } catch (Exception e) {
            log.error("Error creating user profile: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Delete user profile (admin only)
     * DELETE /api/users/{userId}
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUserProfile(
            @PathVariable String userId,
            @RequestHeader("X-User-Role") String role) {
        
        log.info("Delete user profile request: {}", userId);
        
        // Check if admin
        if (!role.equals("SYSTEM_ADMIN")) {
            log.warn("Unauthorized delete attempt - not admin");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        try {
            userService.deleteUserProfile(userId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting user profile: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("User Service is running");
    }
}
