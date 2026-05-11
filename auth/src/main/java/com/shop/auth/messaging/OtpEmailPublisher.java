package com.shop.auth.messaging;

/**
 * Publishes OTP email delivery requests to the message queue.
 *
 * <p>
 * Implementations are expected to be non-blocking from the caller's perspective
 * —
 * the message is handed off to the queue and the method returns immediately,
 * decoupling the OTP generation request path from the email delivery
 * infrastructure.
 * </p>
 */
public interface OtpEmailPublisher {

    /**
     * Publishes an OTP email message to the configured queue.
     *
     * @param message the OTP email payload to enqueue
     * @throws RuntimeException if the message cannot be published (e.g., SQS
     *                          unavailable)
     */
    void publish(OtpEmailMessage message);
}
