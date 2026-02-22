package com.microservices.auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User Role Enum
 * 
 * Defines the available roles in the system:
 * - CUSTOMER: Regular user with limited access
 * - SYSTEM_ADMIN: Administrator with full access
 */
public enum UserRole {
    CUSTOMER,
    SYSTEM_ADMIN
}
