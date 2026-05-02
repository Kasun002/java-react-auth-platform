package com.shop.auth.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends BusinessException {

    public EmailAlreadyExistsException(String email) {
        super("Email already in use: " + email, HttpStatus.CONFLICT);
    }
}
