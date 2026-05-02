package com.shop.auth.service;

public interface EmailService {

    /**
     * Sends a one-time password email to the given address.
     *
     * @param toEmail      recipient email
     * @param toName       recipient display name
     * @param otp          raw 6-digit OTP (plaintext, never stored)
     * @param expiryMinutes OTP validity window in minutes (shown in the email body)
     */
    void sendOtp(String toEmail, String toName, String otp, int expiryMinutes);
}
