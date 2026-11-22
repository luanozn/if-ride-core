package com.ifride.core.shared.rate_limit;

public enum RateLimitOperation {
    LOGIN_EMAIL,
    LOGIN_IP,
    EMAIL_VERIFICATION,
    EMAIL_VERIFICATION_IP,
    PASSWORD_RESET,
    FORGOT_PASSWORD_IP,
    REGISTRATION_IP
}