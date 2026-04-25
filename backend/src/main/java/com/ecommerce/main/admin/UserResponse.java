package com.ecommerce.main.admin;

import com.ecommerce.main.user.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        String email,
        String role,
        String status,
        String gender,
        boolean verified,
        boolean twoFactorEnabled,
        LocalDateTime createdAt
) {
    public static UserResponse from(User u) {
        if (u == null) return null;
        
        String roleName = "INDIVIDUAL";
        try {
            if (u.getRoleType() != null) {
                roleName = u.getRoleType().name();
            }
        } catch (Exception e) {
            // Fallback for corrupted enum values
        }

        boolean suspended = u.getLockedUntil() != null
                && u.getLockedUntil().isAfter(LocalDateTime.now());
        String status = suspended ? "SUSPENDED" : "ACTIVE";
        
        return new UserResponse(
                u.getId(),
                u.getName() != null ? u.getName() : "İsimsiz Kullanıcı",
                u.getEmail() != null ? u.getEmail() : "bilinmeyen@email.com",
                roleName,
                status,
                "UNKNOWN",
                u.isVerified(),
                u.isTwoFactorEnabled(),
                u.getCreatedAt() != null ? u.getCreatedAt() : LocalDateTime.now()
        );
    }
}
