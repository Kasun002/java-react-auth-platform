package com.shop.auth.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;

import com.shop.auth.entity.OtpVerification;
import com.shop.auth.entity.User;
import com.shop.auth.exception.OtpExpiredException;
import com.shop.auth.exception.OtpInvalidException;
import com.shop.auth.exception.OtpMaxAttemptsException;
import com.shop.auth.exception.OtpResendLimitException;
import com.shop.auth.repository.OtpVerificationRepository;
import com.shop.auth.repository.UserRepository;
import com.shop.auth.service.EmailService;
import com.shop.auth.service.OtpService;
import com.shop.auth.utils.HashUtil;
import com.shop.auth.utils.MaskingUtil;
import com.shop.auth.utils.UserStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
    private final UserRepository            userRepository;
    private final EmailService              emailService;

    // ── Generate & Send ───────────────────────────────────────────────────────

    /**
     * REQUIRES_NEW: runs in its own transaction so that a mail-send failure rolls back
     * only the OTP record, not the already-committed user registration.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateAndSend(User user) {
        // Invalidate any existing unused OTPs before issuing a new one.
        // A previously captured (but undelivered or intercepted) OTP must not remain valid.
        otpVerificationRepository.invalidateAllUnusedForUser(user);

        String rawOtp = generateRawOtp();

        OtpVerification record = new OtpVerification();
        record.setUser(user);
        record.setOtpHash(HashUtil.sha256Hex(rawOtp));
        record.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes));
        otpVerificationRepository.save(record);

        emailService.sendOtp(user.getEmail(), user.getName(), rawOtp, otpExpiryMinutes);
        log.info("OTP generated and sent for email=[{}]", MaskingUtil.maskEmail(user.getEmail()));
    }

    // ── Verify ────────────────────────────────────────────────────────────────

    /**
     * noRollbackFor ensures the incremented attempt counter is committed even when
     * an OTP mismatch exception is thrown.
     */
    @Override
    @Transactional(noRollbackFor = com.shop.auth.exception.BusinessException.class)
    public void verify(String email, String otp) {
        log.debug("OTP verify attempt for email=[{}]", MaskingUtil.maskEmail(email));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("OTP verify — email not found: [{}]", MaskingUtil.maskEmail(email));
                    return new OtpInvalidException();   // same error — prevents enumeration
                });

        if (user.getStatus() == UserStatus.ACTIVE) {
            log.info("OTP verify — account already active: [{}]", MaskingUtil.maskEmail(email));
            return;                                     // idempotent: already verified
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

        // Increment attempts pessimistically before checking — count is committed even on mismatch
        record.setAttempts(record.getAttempts() + 1);
        otpVerificationRepository.save(record);

        // Constant-time comparison — prevents timing side-channel on the hash comparison.
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
                    return new OtpInvalidException();   // same error — prevents enumeration
                });

        if (user.getStatus() == UserStatus.ACTIVE) {
            log.info("OTP resend — account already active: [{}]", MaskingUtil.maskEmail(email));
            return;                                     // idempotent
        }

        if (user.getStatus() != UserStatus.NEW) {
            log.warn("OTP resend — account not in NEW status: email=[{}] status=[{}]",
                    MaskingUtil.maskEmail(email), user.getStatus());
            throw new OtpInvalidException();
        }

        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentCount = otpVerificationRepository.countByUserAndCreatedAtAfter(user, oneHourAgo);
        if (recentCount >= maxResendsPerHour) {
            log.warn("OTP resend — rate limit hit: email=[{}] count=[{}]",
                    MaskingUtil.maskEmail(email), recentCount);
            throw new OtpResendLimitException();
        }

        generateAndSend(user);
        log.info("OTP resent for email=[{}]", MaskingUtil.maskEmail(email));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static String generateRawOtp() {
        // Full 10^6 entropy (000000–999999). Zero-padded to always produce exactly 6 digits.
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }

}
