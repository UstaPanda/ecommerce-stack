package com.ecommerce.main.audit;

public enum AuditEventType {
    // Auth
    REGISTER,
    REGISTER_EMAIL_CONFLICT,
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    LOGOUT,
    GOOGLE_LOGIN,

    // Email
    EMAIL_VERIFIED,
    VERIFICATION_RESENT,

    // Password
    PASSWORD_RESET_REQUEST,
    PASSWORD_RESET_SUCCESS,

    // Account
    ACCOUNT_LOCKED,
    ACCOUNT_UNLOCKED,

    // Token
    TOKEN_REFRESHED,
    DEVICE_REVOKED,

    // 2FA
    TWO_FA_ENABLED,
    TWO_FA_DISABLED,
    TWO_FA_FAILED,
    TWO_FA_SUCCESS
}
