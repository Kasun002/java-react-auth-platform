package com.org.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a token presented to a service endpoint is invalid, expired,
 * the wrong type, or has already been revoked.
 *
 * <p>A generic 401 message is used intentionally — no specifics about
 * <em>why</em> the token was rejected are surfaced to the caller.</p>
 */
public class InvalidTokenException extends BusinessException {

    public InvalidTokenException() {
        super("Invalid or expired token", HttpStatus.UNAUTHORIZED);
    }
}
