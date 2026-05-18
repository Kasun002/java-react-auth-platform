package com.org.auth.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.org.auth.entity.OtpVerification;
import com.org.auth.entity.User;
import com.org.auth.exception.OtpExpiredException;
import com.org.auth.exception.OtpInvalidException;
import com.org.auth.exception.OtpMaxAttemptsException;
import com.org.auth.exception.OtpResendLimitException;
import com.org.auth.messaging.OtpEmailMessage;
import com.org.auth.messaging.OtpEmailPublisher;
import com.org.auth.repository.OtpVerificationRepository;
import com.org.auth.repository.UserRepository;
import com.org.auth.service.OtpRateLimitService;
import com.org.auth.service.OtpService;
import com.org.auth.utils.HashUtil;
import com.org.auth.utils.MaskingUtil;
import com.org.auth.utils.Otp;
import com.org.auth.utils.UserStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    @Value("${app.otp.expiry-minutes}")
    private int otpExpiryMinutes;

    @Value("${app.otp.max-attempts}")
    private int maxAttempts;

    @Value("${app.otp.max-resends-per-hour}")
    private int maxResendsPerHour;

    private final OtpVerificationRepository otpVerificationRepository;
    private final UserRepository userRepository;
    private final OtpEmailPublisher otpEmailPublisher;
    private final OtpRateLimitService otpRateLimitService;

    // ── Generate & Send ───────────────────────────────────────────────────────

    /**
     * Joins the caller's transaction (REQUIRED) so the OTP record is inserted in
     * the same
     * transaction as the user row — avoiding the FK violation that REQUIRES_NEW
     * caused
     * (the inner transaction couldn't see the uncommitted user row in the outer
     * transaction).
     *
     * SQS publish is wrapped in its own try-catch so a queue failure does not mark
     * the
     * transaction rollback-only. The OTP record is committed with the user; the
     * account
     * holder can retry delivery via {@code /auth/resend-otp}.
     */
    @Override
    @Transactional
    public void generateAndSend(User user) {
        // Invalidate any existing unused OTPs before issuing a new one.
        // A previously captured (but undelivered or intercepted) OTP must not remain
        // valid.
        otpVerificationRepository.invalidateAllUnusedForUser(user);

        String rawOtp = Otp.generateRawOtp();
        // System.out.println("OTP" + rawOtp);
        OtpVerification record = new OtpVerification();
        record.setUser(user);
        record.setOtpHash(HashUtil.sha256Hex(rawOtp));
        record.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes));
        otpVerificationRepository.save(record);

        // Publish to SQS — the consumer delivers via SES asynchronously.
        // Swallow publish failures: the OTP record is already persisted, so the user
        // can
        // trigger a fresh delivery via /auth/resend-otp without re-registering.
        try {
            OtpEmailMessage message = new OtpEmailMessage(
                    UUID.randomUUID().toString(),
                    user.getEmail(),
                    user.getName(),
                    rawOtp,
                    otpExpiryMinutes,
                    Instant.now());
            otpEmailPublisher.publish(message);
            log.info("OTP generated and queued for delivery: email=[{}]",
                    MaskingUtil.maskEmail(user.getEmail()));
        } catch (Exception e) {
            log.error(
                    "OTP email publish failed for email=[{}] — OTP record saved, user can resend via /auth/resend-otp",
                    MaskingUtil.maskEmail(user.getEmail()), e);
        }
    }

    // ── Verify ────────────────────────────────────────────────────────────────

    /**
     * noRollbackFor ensures the incremented attempt counter is committed even when
     * an OTP mismatch exception is thrown.
     */
    @Override
    @Transactional(noRollbackFor = com.org.auth.exception.BusinessException.class)
    public void verify(String email, String otp) {
        log.debug("OTP verify attempt for email=[{}]", MaskingUtil.maskEmail(email));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("OTP verify — email not found: [{}]", MaskingUtil.maskEmail(email));
                    return new OtpInvalidException(); // same error — prevents enumeration
                });

        if (user.getStatus() == UserStatus.ACTIVE) {
            log.info("OTP verify — account already active: [{}]", MaskingUtil.maskEmail(email));
            return; // idempotent: already verified
        }

        if (user.getStatus() != UserStatus.NEW) {
            log.warn("OTP verify — account in non-verifiable status=[{}] email=[{}]",
                    user.getStatus(), MaskingUtil.maskEmail(email));
            throw new OtpInvalidException();
        }

        OtpVerification record = otpVerificationRepository
                .findTopByUserAndUsedFalseOrderByCreatedAtDesc(user)
                .orElseThrow(() -> {
                    log.warn("OTP verify — no active OTP record: [{}]", MaskingUtil.maskEmail(email));
                    return new OtpExpiredException();
                });

        if (LocalDateTime.now().isAfter(record.getExpiresAt())) {
            log.warn("OTP verify — OTP expired: [{}]", MaskingUtil.maskEmail(email));
            throw new OtpExpiredException();
        }

        if (record.getAttempts() >= maxAttempts) {
            log.warn("OTP verify — max attempts exceeded: email=[{}]", MaskingUtil.maskEmail(email));
            throw new OtpMaxAttemptsException();
        }

        // Increment attempts pessimistically before checking — count is committed even
        // on mismatch
        record.setAttempts(record.getAttempts() + 1);
        otpVerificationRepository.save(record);

        // Constant-time comparison — prevents timing side-channel on the hash
        // comparison.
        // MessageDigest.isEqual is guaranteed constant-time for equal-length inputs;
        // both operands are always 64-byte UTF-8 hex strings.
        boolean hashMatch = MessageDigest.isEqual(
                HashUtil.sha256Hex(otp).getBytes(StandardCharsets.UTF_8),
                record.getOtpHash().getBytes(StandardCharsets.UTF_8));
        if (!hashMatch) {
            log.warn("OTP verify — wrong OTP: email=[{}] attempt=[{}]",
                    MaskingUtil.maskEmail(email), record.getAttempts());
            throw new OtpInvalidException();
        }

        record.setUsed(true);
        otpVerificationRepository.save(record);

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        log.info("Account activated: email=[{}]", MaskingUtil.maskEmail(email));
    }

    // ── Resend ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void resend(String email) {
        log.debug("OTP resend request for email=[{}]", MaskingUtil.maskEmail(email));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("OTP resend — email not found: [{}]", MaskingUtil.maskEmail(email));
                    return new OtpInvalidException(); // same error — prevents enumeration
                });

        if (user.getStatus() == UserStatus.ACTIVE) {
            log.info("OTP resend — account already active: [{}]", MaskingUtil.maskEmail(email));
            return; // idempotent
        }

        if (user.getStatus() != UserStatus.NEW) {
            log.warn("OTP resend — account not in NEW status: email=[{}] status=[{}]",
                    MaskingUtil.maskEmail(email), user.getStatus());
            throw new OtpInvalidException();
        }

        if (!otpRateLimitService.checkAndIncrementResend(user.getId(), maxResendsPerHour)) {
            log.warn("OTP resend — rate limit hit: email=[{}]", MaskingUtil.maskEmail(email));
            throw new OtpResendLimitException();
        }

        generateAndSend(user);
        log.info("OTP resent for email=[{}]", MaskingUtil.maskEmail(email));
    }

}
