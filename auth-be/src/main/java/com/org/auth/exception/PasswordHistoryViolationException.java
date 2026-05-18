package com.org.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a user attempts to set a password that matches one of their
 * recent historical passwords (PCI-DSS Req 8.3.6 / NIST 800-63B §5.1.1).
 */
public class PasswordHistoryViolationException extends BusinessException {

    public PasswordHistoryViolationException(int historyCount) {
        super("Password cannot be the same as any of your last " + historyCount
              + " passwords.", HttpStatus.BAD_REQUEST);
    }
}
