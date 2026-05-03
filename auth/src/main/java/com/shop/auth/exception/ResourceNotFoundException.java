package com.shop.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested resource (role, group, permission, user) does not exist.
 * HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " not found with id: " + id, HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
