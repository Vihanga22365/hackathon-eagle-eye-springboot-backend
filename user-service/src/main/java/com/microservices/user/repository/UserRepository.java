package com.microservices.user.repository;

import com.google.firebase.database.*;
import com.microservices.user.model.UserProfile;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * User Repository
 * 
 * Manages user profile data in Firebase Realtime Database.
 * Uses CompletableFuture to handle Firebase's asynchronous operations.
 * 
 * Database Structure:
 * users/
 *   {userId}/
 *     email: "user@example.com"
 *     fullName: "John Doe"
 *     phoneNumber: "+1234567890"
 *     address: "123 Main St"
 *     role: "CUSTOMER"
 *     createdAt: 1234567890
 *     updatedAt: 1234567890
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class UserRepository {

    private final FirebaseDatabase firebaseDatabase;
    private static final String USERS_PATH = "users";

    /**
     * Save or update user profile
     */
    @CircuitBreaker(name = "userService", fallbackMethod = "saveUserFallback")
    public UserProfile saveUser(UserProfile userProfile) {
        log.info("Saving user profile: {}", userProfile.getUserId());
        
        try {
            CompletableFuture<UserProfile> future = new CompletableFuture<>();
            
            DatabaseReference userRef = firebaseDatabase.getReference(USERS_PATH)
                    .child(userProfile.getUserId());
            
            userProfile.setUpdatedAt(System.currentTimeMillis());
            if (userProfile.getCreatedAt() == null) {
                userProfile.setCreatedAt(System.currentTimeMillis());
            }
            
            userRef.setValue(userProfile, (error, ref) -> {
                if (error != null) {
                    log.error("Error saving user: {}", error.getMessage());
                    future.completeExceptionally(new RuntimeException("Failed to save user"));
                } else {
                    log.info("User saved successfully: {}", userProfile.getUserId());
                    future.complete(userProfile);
                }
            });
            
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error saving user to Firebase: {}", e.getMessage());
            throw new RuntimeException("Failed to save user", e);
        }
    }

    /**
     * Find user by ID
     */
    @CircuitBreaker(name = "userService", fallbackMethod = "findUserByIdFallback")
    public UserProfile findUserById(String userId) {
        log.info("Finding user by ID: {}", userId);
        
        try {
            CompletableFuture<UserProfile> future = new CompletableFuture<>();
            
            DatabaseReference userRef = firebaseDatabase.getReference(USERS_PATH)
                    .child(userId);
            
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        UserProfile user = snapshot.getValue(UserProfile.class);
                        log.info("User found: {}", userId);
                        future.complete(user);
                    } else {
                        log.warn("User not found: {}", userId);
                        future.complete(null);
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    log.error("Error fetching user: {}", error.getMessage());
                    future.completeExceptionally(new RuntimeException("Failed to fetch user"));
                }
            });
            
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error fetching user from Firebase: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch user", e);
        }
    }

    /**
     * Find all users (admin only)
     */
    @CircuitBreaker(name = "userService", fallbackMethod = "findAllUsersFallback")
    public List<UserProfile> findAllUsers() {
        log.info("Finding all users");
        
        try {
            CompletableFuture<List<UserProfile>> future = new CompletableFuture<>();
            
            DatabaseReference usersRef = firebaseDatabase.getReference(USERS_PATH);
            
            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    List<UserProfile> users = new ArrayList<>();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        UserProfile user = child.getValue(UserProfile.class);
                        if (user != null) {
                            users.add(user);
                        }
                    }
                    log.info("Found {} users", users.size());
                    future.complete(users);
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    log.error("Error fetching users: {}", error.getMessage());
                    future.completeExceptionally(new RuntimeException("Failed to fetch users"));
                }
            });
            
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error fetching users from Firebase: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch users", e);
        }
    }

    /**
     * Delete user
     */
    @CircuitBreaker(name = "userService", fallbackMethod = "deleteUserFallback")
    public void deleteUser(String userId) {
        log.info("Deleting user: {}", userId);
        
        try {
            CompletableFuture<Void> future = new CompletableFuture<>();
            
            DatabaseReference userRef = firebaseDatabase.getReference(USERS_PATH)
                    .child(userId);
            
            userRef.removeValue((error, ref) -> {
                if (error != null) {
                    log.error("Error deleting user: {}", error.getMessage());
                    future.completeExceptionally(new RuntimeException("Failed to delete user"));
                } else {
                    log.info("User deleted successfully: {}", userId);
                    future.complete(null);
                }
            });
            
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error deleting user from Firebase: {}", e.getMessage());
            throw new RuntimeException("Failed to delete user", e);
        }
    }

    // Fallback methods for circuit breaker
    
    private UserProfile saveUserFallback(UserProfile userProfile, Exception e) {
        log.error("Circuit breaker activated for saveUser: {}", e.getMessage());
        throw new RuntimeException("User service is currently unavailable");
    }

    private UserProfile findUserByIdFallback(String userId, Exception e) {
        log.error("Circuit breaker activated for findUserById: {}", e.getMessage());
        throw new RuntimeException("User service is currently unavailable");
    }

    private List<UserProfile> findAllUsersFallback(Exception e) {
        log.error("Circuit breaker activated for findAllUsers: {}", e.getMessage());
        throw new RuntimeException("User service is currently unavailable");
    }

    private void deleteUserFallback(String userId, Exception e) {
        log.error("Circuit breaker activated for deleteUser: {}", e.getMessage());
        throw new RuntimeException("User service is currently unavailable");
    }
}
