package com.org.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an authenticated user attempts an operation they are not authorised
 * to perform. HTTP 403 Forbidden.
 *
 * <p>Use this for explicit service-layer 403 throws. Spring Security's own
 * {@code @PreAuthorize} failures throw {@code org.springframework.security.access.AccessDeniedException}
 * which is handled separately in {@code GlobalExceptionHandler}.</p>
 */
public class AccessDeniedException extends BusinessException {

    public AccessDeniedException(String action) {
        super("Access denied: " + action, HttpStatus.FORBIDDEN);
    }

    public AccessDeniedException() {
        super("Access denied", HttpStatus.FORBIDDEN);
    }
}
