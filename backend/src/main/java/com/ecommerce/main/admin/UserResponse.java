package com.ecommerce.main.admin;

import com.ecommerce.main.user.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        String email,
        String role,
        String roleType,
        String status,
        String provider,
        boolean verified,
        boolean twoFactorEnabled,
        int failedLoginAttempts,
        LocalDateTime lockedUntil,
        LocalDateTime createdAt
) {
    public static UserResponse from(User u) {
        String roleName = u.getRoleType().name();
        boolean suspended = u.getLockedUntil() != null
                && u.getLockedUntil().isAfter(LocalDateTime.now().plusYears(50));
        String status = suspended ? "SUSPENDED" : "ACTIVE";
        return new UserResponse(
                u.getId(),
                u.getName(),
                u.getEmail(),
                roleName,
                roleName,
                status,
                u.getProvider().name(),
                u.isVerified(),
                u.isTwoFactorEnabled(),
                u.getFailedLoginAttempts(),
                u.getLockedUntil(),
                u.getCreatedAt()
        );
    }
}
