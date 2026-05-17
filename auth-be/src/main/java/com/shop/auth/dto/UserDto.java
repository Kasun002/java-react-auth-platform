package com.shop.auth.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.shop.auth.utils.AuthProvider;
import com.shop.auth.utils.Gender;
import com.shop.auth.utils.UserStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Standardised read-only user representation returned in API responses.
 * Never includes security-sensitive fields (password, failedLoginAttempts,
 * lockedUntil).
 */
@Data
@Schema(description = "User profile data returned in API responses")
public class UserDto {

        // ── Identity ──────────────────────────────────────────────────────────────

        @Schema(description = "User ID", example = "42")
        private Long id;

        @Schema(description = "Full name", example = "John Doe")
        private String name;

        @Schema(description = "Email address", example = "john.doe@example.com")
        private String email;

        @Schema(description = "Contact phone number", example = "+94771234567")
        private String phone;

        // ── Account state ─────────────────────────────────────────────────────────

        @Schema(description = "Account status", example = "ACTIVE")
        private UserStatus status;

        @Schema(description = "How this account was originally provisioned", example = "LOCAL")
        private AuthProvider authProvider;

        // ── Optional profile fields ───────────────────────────────────────────────

        @Schema(description = "Date of birth", example = "1990-06-15")
        private LocalDate dateOfBirth;

        @Schema(description = "Gender", example = "MALE")
        private Gender gender;

        @Schema(description = "Profile picture URL", example = "https://cdn.example.com/profiles/42.jpg")
        private String profilePictureUrl;

        // ── Timestamps ────────────────────────────────────────────────────────────

        @Schema(description = "Timestamp of the last successful login")
        private LocalDateTime lastLoginAt;

        @Schema(description = "Account creation timestamp")
        private LocalDateTime createdAt;

        @Schema(description = "Last profile update timestamp")
        private LocalDateTime updatedAt;

        // ── RBAC — computed at login time from group/role membership ─────────────

        @Schema(description = "User group memberships", example = "[\"RETAIL_CUSTOMER\"]")
        private List<String> groups;

        @Schema(description = "Roles from group membership and direct assignments", example = "[\"ROLE_CUSTOMER_BASIC\"]")
        private List<String> roles;

        @Schema(description = "Effective permission codes — union of all assigned roles", example = "[\"ACCOUNT_VIEW\", \"TRANSACTION_VIEW\"]")
        private List<String> effectivePermissions;

        // ── Relations ─────────────────────────────────────────────────────────────

        @Schema(description = "User addresses")
        private List<AddressDto> addresses;
}
