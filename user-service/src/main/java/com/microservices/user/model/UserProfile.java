package com.microservices.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User Profile Model
 * 
 * Represents user profile information stored in Firebase Realtime Database.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
    
    private String userId;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String address;
    private String role;
    private Long createdAt;
    private Long updatedAt;
}
