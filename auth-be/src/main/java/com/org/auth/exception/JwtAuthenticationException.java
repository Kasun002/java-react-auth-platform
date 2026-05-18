package com.org.auth.exception;

/**
 * Thrown by {@code JwtAuthenticationFilter} when a token is absent, malformed,
 * expired, or the wrong type. Extends {@link RuntimeException} (not
 * {@link BusinessException}) because it is handled directly by the filter or
 * Spring Security's {@code AuthenticationEntryPoint} — never by
 * {@code GlobalExceptionHandler}.
 */
public class JwtAuthenticationException extends RuntimeException {

    public JwtAuthenticationException(String message) {
        super(message);
    }

    public JwtAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
