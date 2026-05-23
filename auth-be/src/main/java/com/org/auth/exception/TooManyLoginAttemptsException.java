package com.org.auth.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a client IP exceeds the maximum login attempts within the rate-limit window. */
public class TooManyLoginAttemptsException extends BusinessException {

    public TooManyLoginAttemptsException() {
        super("Too many login attempts. Please try again later.", HttpStatus.TOO_MANY_REQUESTS);
    }
}
