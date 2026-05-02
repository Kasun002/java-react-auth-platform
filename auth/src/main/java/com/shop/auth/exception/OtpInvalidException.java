package com.shop.auth.exception;

import org.springframework.http.HttpStatus;

/** Thrown when the submitted OTP does not match the stored hash, or the email is unknown. */
public class OtpInvalidException extends BusinessException {

    public OtpInvalidException() {
        super("Invalid OTP or email address", HttpStatus.BAD_REQUEST);
    }
}
