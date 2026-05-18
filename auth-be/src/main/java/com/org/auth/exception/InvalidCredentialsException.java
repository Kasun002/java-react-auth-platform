package com.org.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown for both "user not found" and "wrong password" — same 401 response
 * prevents user-enumeration attacks.
 */
public class InvalidCredentialsException extends BusinessException {

    public InvalidCredentialsException() {
        super("Invalid email or password", HttpStatus.UNAUTHORIZED);
    }
}
