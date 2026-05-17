package com.shop.auth.service;

import com.shop.auth.entity.User;

public interface OtpService {

    /**
     * Generates a 6-digit OTP, persists its hash, and emails the raw value to the user.
     * Runs in its own transaction (REQUIRES_NEW) so that a mail-send failure does not
     * roll back an already-committed user registration.
     */
    void generateAndSend(User user);

    /**
     * Validates the submitted OTP against the latest unused record for the given email.
     * On success, sets the user status to ACTIVE.
     */
    void verify(String email, String otp);

    /**
     * Generates and sends a fresh OTP, subject to per-hour resend rate-limiting.
     */
    void resend(String email);
}
