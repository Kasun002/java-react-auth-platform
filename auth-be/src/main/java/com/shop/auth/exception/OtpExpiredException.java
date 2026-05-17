package com.shop.auth.exception;

import org.springframework.http.HttpStatus;

/** Thrown when no active (unused, non-expired) OTP record exists for the user. */
public class OtpExpiredException extends BusinessException {

    public OtpExpiredException() {
        super("OTP has expired or does not exist. Please request a new one.", HttpStatus.BAD_REQUEST);
    }
}
