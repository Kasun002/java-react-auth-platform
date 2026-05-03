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

    /**
     * Sends a password reset email containing a single-use reset link.
     *
     * @param toEmail        recipient email address
     * @param toName         recipient display name
     * @param resetLink      the full reset URL (includes the raw token as a query param)
     * @param expiryMinutes  how long the link is valid (shown in the email body)
     */
    void sendPasswordResetEmail(String toEmail, String toName, String resetLink, int expiryMinutes);
}
