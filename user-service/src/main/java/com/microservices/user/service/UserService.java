package com.microservices.user.service;

import com.microservices.user.dto.UpdateProfileRequest;
import com.microservices.user.model.UserProfile;
import com.microservices.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * User Service
 * 
 * Business logic for user profile management.
 * Enforces access control rules:
 * - Users can view and update their own profile
 * - Admins can view all profiles
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * Get user profile by ID
     * Access control checked in controller
     */
    public UserProfile getUserProfile(String userId) {
        log.info("Getting user profile: {}", userId);
        UserProfile profile = userRepository.findUserById(userId);
        
        if (profile == null) {
            throw new RuntimeException("User not found: " + userId);
        }
        
        return profile;
    }

    /**
     * Get all user profiles (admin only)
     */
    public List<UserProfile> getAllUsers() {
        log.info("Getting all user profiles");
        return userRepository.findAllUsers();
    }

    /**
     * Update user profile
     * Access control checked in controller
     */
    public UserProfile updateUserProfile(String userId, UpdateProfileRequest request) {
        log.info("Updating user profile: {}", userId);
        
        // Get existing profile
        UserProfile existingProfile = userRepository.findUserById(userId);
        
        if (existingProfile == null) {
            throw new RuntimeException("User not found: " + userId);
        }
        
        // Update fields
        existingProfile.setFullName(request.getFullName());
        existingProfile.setPhoneNumber(request.getPhoneNumber());
        existingProfile.setAddress(request.getAddress());
        
        // Save updated profile
        return userRepository.saveUser(existingProfile);
    }

    /**
     * Create user profile (called after registration)
     */
    public UserProfile createUserProfile(UserProfile userProfile) {
        log.info("Creating user profile: {}", userProfile.getUserId());
        return userRepository.saveUser(userProfile);
    }

    /**
     * Delete user profile
     */
    public void deleteUserProfile(String userId) {
        log.info("Deleting user profile: {}", userId);
        userRepository.deleteUser(userId);
    }
}
