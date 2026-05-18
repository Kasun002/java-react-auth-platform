package com.org.auth.exception;

import org.springframework.http.HttpStatus;

/** Thrown when the user exceeds the maximum allowed OTP resend requests within an hour. */
public class OtpResendLimitException extends BusinessException {

    public OtpResendLimitException() {
        super("Too many OTP requests. Please wait before requesting again.", HttpStatus.TOO_MANY_REQUESTS);
    }
}
