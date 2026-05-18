package com.org.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when deleting a resource that is still referenced by other entities.
 * HTTP 409 Conflict.
 */
public class ConflictException extends BusinessException {

    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
