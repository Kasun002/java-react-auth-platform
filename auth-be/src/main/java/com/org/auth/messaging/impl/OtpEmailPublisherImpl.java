package com.org.auth.messaging.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.auth.messaging.OtpEmailMessage;
import com.org.auth.messaging.OtpEmailPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/**
 * SQS-backed {@link OtpEmailPublisher}.
 *
 * <p>
 * Serialises the message to JSON and sends it to the configured SQS queue.
 * On failure the exception propagates to the caller; within
 * {@code generateAndSend}'s
 * {@code REQUIRES_NEW} transaction this rolls back the OTP record, keeping the
 * database consistent — the user can retry via {@code /auth/resend-otp}.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OtpEmailPublisherImpl implements OtpEmailPublisher {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${app.messaging.otp-email-queue-url}")
    private String queueUrl;

    @Override
    public void publish(OtpEmailMessage message) {
        String body;
        try {
            body = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialise OTP email message", e);
        }

        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(body)
                .build());

        log.debug("OTP email queued: messageId=[{}]", message.messageId());
    }
}
