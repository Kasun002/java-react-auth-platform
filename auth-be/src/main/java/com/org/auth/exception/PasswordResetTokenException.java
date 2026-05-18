package com.org.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a password reset token is missing, expired, or has already been used.
 * A generic message is returned intentionally — no hint about which check failed.
 */
public class PasswordResetTokenException extends BusinessException {

    public PasswordResetTokenException() {
        super("Invalid or expired password reset token.", HttpStatus.BAD_REQUEST);
    }
}
