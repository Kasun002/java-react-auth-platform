package com.shop.auth.exception;

import org.springframework.http.HttpStatus;

/** Thrown when the user has exhausted verification attempts for the current OTP. */
public class OtpMaxAttemptsException extends BusinessException {

    public OtpMaxAttemptsException() {
        super("Maximum OTP verification attempts exceeded. Please request a new OTP.", HttpStatus.TOO_MANY_REQUESTS);
    }
}
