package com.shop.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an Azure AD ID token cannot be validated or the AD login flow fails.
 *
 * <p>Reasons include: invalid/expired token, unknown issuer, audience mismatch,
 * missing required claims, or the AD login feature being disabled.
 */
public class AdAuthenticationException extends BusinessException {

    public AdAuthenticationException(String reason) {
        super("AD authentication failed: " + reason, HttpStatus.UNAUTHORIZED);
    }
}
