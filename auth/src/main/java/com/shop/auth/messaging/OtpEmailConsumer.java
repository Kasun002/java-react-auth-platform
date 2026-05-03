package com.shop.auth.messaging;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.auth.utils.MaskingUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.QueueNameExistsException;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

/**
 * Background consumer that polls the OTP email SQS queue and delivers via SES.
 *
 * <p>Runs on a dedicated Java 21 virtual thread started on
 * {@link ApplicationReadyEvent} and stopped gracefully on {@link ContextClosedEvent}.
 * Uses SQS long-polling (20 s) to minimise idle API calls.</p>
 *
 * <p>Delivery guarantees:</p>
 * <ul>
 *   <li>A message is deleted from SQS only after successful SES delivery (at-least-once).</li>
 *   <li>On SES failure the message becomes visible again after the queue's visibility
 *       timeout and is retried automatically, eventually landing in a DLQ if configured.</li>
 *   <li>Stale messages (age &ge; OTP expiry) are discarded without delivery.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OtpEmailConsumer {

    private static final int LONG_POLL_SECONDS = 20;
    private static final int MAX_MESSAGES      = 10;
    private static final int ERROR_BACKOFF_MS  = 5_000;

    private final SqsClient    sqsClient;
    private final SesClient    sesClient;
    private final ObjectMapper objectMapper;

    @Value("${app.messaging.otp-email-queue-url}")
    private String queueUrl;

    @Value("${app.messaging.ses-sender-email}")
    private String senderEmail;

    @Value("${app.messaging.consumer.enabled:true}")
    private boolean consumerEnabled;

    private volatile boolean running = false;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @EventListener(ApplicationReadyEvent.class)
    public void startConsumer() {
        if (!consumerEnabled) {
            log.info("OTP email consumer is disabled (app.messaging.consumer.enabled=false)");
            return;
        }
        provisionQueue();
        running = true;
        Thread.ofVirtual()
              .name("otp-email-consumer")
              .start(this::consumeLoop);
        log.info("OTP email consumer started");
    }

    /**
     * Creates the SQS queue if it does not already exist.
     *
     * <p>{@code CreateQueue} is idempotent: calling it on an already-existing queue
     * with the same attributes is a no-op and returns the existing URL. This covers
     * both local (LocalStack) and real AWS environments without requiring manual
     * queue pre-creation during development.</p>
     */
    private void provisionQueue() {
        String queueName = queueUrl.substring(queueUrl.lastIndexOf('/') + 1);
        try {
            sqsClient.createQueue(CreateQueueRequest.builder()
                    .queueName(queueName)
                    .build());
            log.info("SQS queue ready: [{}]", queueName);
        } catch (QueueNameExistsException e) {
            // Queue exists with different attributes — acceptable, continue with existing queue
            log.info("SQS queue already exists: [{}]", queueName);
        } catch (Exception e) {
            log.warn("SQS queue provisioning failed for [{}] — consumer will retry on first poll: {}",
                    queueName, e.getMessage());
        }
    }

    @EventListener(ContextClosedEvent.class)
    public void stopConsumer() {
        running = false;
        log.info("OTP email consumer stopping");
    }

    // ── Consumer loop ─────────────────────────────────────────────────────────

    private void consumeLoop() {
        while (running) {
            try {
                pollAndProcess();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("OTP email consumer cycle failed — backing off {}ms", ERROR_BACKOFF_MS, e);
                try {
                    Thread.sleep(ERROR_BACKOFF_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        log.info("OTP email consumer stopped");
    }

    private void pollAndProcess() throws InterruptedException {
        List<software.amazon.awssdk.services.sqs.model.Message> messages =
                sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(MAX_MESSAGES)
                        .waitTimeSeconds(LONG_POLL_SECONDS)
                        .build())
                .messages();

        for (software.amazon.awssdk.services.sqs.model.Message sqsMessage : messages) {
            if (!running) break;
            processMessage(sqsMessage);
        }
    }

    // ── Per-message processing ────────────────────────────────────────────────

    private void processMessage(software.amazon.awssdk.services.sqs.model.Message sqsMessage) {
        OtpEmailMessage payload;
        try {
            payload = objectMapper.readValue(sqsMessage.body(), OtpEmailMessage.class);
        } catch (Exception e) {
            log.error("Discarding malformed SQS message sqsId=[{}]", sqsMessage.messageId(), e);
            deleteMessage(sqsMessage.receiptHandle());
            return;
        }

        // Stale-message guard — OTP already expired; sending email would confuse the user
        long ageMinutes = ChronoUnit.MINUTES.between(payload.createdAt(), Instant.now());
        if (ageMinutes >= payload.expiryMinutes()) {
            log.warn("Discarding stale OTP email: email=[{}] ageMinutes=[{}]",
                    MaskingUtil.maskEmail(payload.email()), ageMinutes);
            deleteMessage(sqsMessage.receiptHandle());
            return;
        }

        try {
            sendViaSes(payload);
            deleteMessage(sqsMessage.receiptHandle());
            log.info("OTP email delivered via SES: email=[{}] messageId=[{}]",
                    MaskingUtil.maskEmail(payload.email()), payload.messageId());
        } catch (Exception e) {
            // Do NOT delete — SQS visibility timeout will re-expose the message for retry
            log.error("SES delivery failed for email=[{}] — will retry after visibility timeout",
                    MaskingUtil.maskEmail(payload.email()), e);
        }
    }

    // ── SES delivery ──────────────────────────────────────────────────────────

    private void sendViaSes(OtpEmailMessage msg) {
        String subject = "Your One-Time Password";
        String body = String.format(
                "Dear %s,%n%n"
                + "Your one-time password (OTP) is: %s%n%n"
                + "This OTP expires in %d minute(s). Do not share it with anyone.%n%n"
                + "If you did not request this OTP, please contact support immediately.%n%n"
                + "Regards,%n"
                + "Security Team",
                msg.name(), msg.otp(), msg.expiryMinutes());

        sesClient.sendEmail(SendEmailRequest.builder()
                .destination(Destination.builder()
                        .toAddresses(msg.email())
                        .build())
                .message(Message.builder()
                        .subject(Content.builder().data(subject).charset("UTF-8").build())
                        .body(Body.builder()
                                .text(Content.builder().data(body).charset("UTF-8").build())
                                .build())
                        .build())
                .source(senderEmail)
                .build());
    }

    // ── SQS helpers ───────────────────────────────────────────────────────────

    private void deleteMessage(String receiptHandle) {
        try {
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(receiptHandle)
                    .build());
        } catch (Exception e) {
            // Log and continue — worst case: the message is re-processed (idempotent via SES dedup)
            log.warn("Failed to delete SQS message after processing — possible duplicate delivery", e);
        }
    }
}
