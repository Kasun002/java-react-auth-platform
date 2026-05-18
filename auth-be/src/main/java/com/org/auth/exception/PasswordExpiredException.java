package com.org.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a user's password has exceeded the maximum age (PCI-DSS Req 8.3.9).
 * The user must change their password before they can authenticate.
 */
public class PasswordExpiredException extends BusinessException {

    public PasswordExpiredException(int maxAgeDays) {
        super("Your password has not been changed in over " + maxAgeDays + " days. "
              + "Please change your password to continue.", HttpStatus.FORBIDDEN);
    }
}
