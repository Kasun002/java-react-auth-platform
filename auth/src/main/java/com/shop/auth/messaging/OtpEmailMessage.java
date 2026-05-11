package com.shop.auth.messaging;

import java.time.Instant;

/**
 * Immutable SQS payload for an OTP email delivery request.
 *
 * <p>
 * The {@code otp} field holds the raw one-time password only for the brief
 * in-flight window inside SQS. Enable SQS server-side encryption (SSE-KMS) in
 * production to protect this value at rest.
 * </p>
 *
 * @param messageId     UUID for idempotency / deduplication tracking
 * @param email         recipient email address
 * @param name          recipient display name for the email greeting
 * @param otp           raw one-time password (ephemeral — consumed and deleted
 *                      immediately)
 * @param expiryMinutes OTP lifetime in minutes; consumer discards stale
 *                      messages past this age
 * @param createdAt     message creation timestamp used for stale-message
 *                      detection
 */
public record OtpEmailMessage(
                String messageId,
                String email,
                String name,
                String otp,
                int expiryMinutes,
                Instant createdAt) {
}
