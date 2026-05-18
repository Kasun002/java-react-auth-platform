package com.org.auth.job;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.org.auth.repository.OtpVerificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * One-shot cleanup job that deletes expired OTP records from the database.
 *
 * <p>Activated exclusively under the {@code cleanup} Spring profile, which is set
 * by the Kubernetes CronJob pod via {@code SPRING_PROFILES_ACTIVE=cleanup}.
 * The main auth-service pods never load this bean.</p>
 *
 * <p>Lifecycle:</p>
 * <ol>
 *   <li>Spring context starts (no web server — {@code spring.main.web-application-type=none})</li>
 *   <li>{@link #run} executes the bulk DELETE inside a single transaction</li>
 *   <li>Spring context closes; JVM exits with code {@code 0} on success,
 *       {@code 1} if an uncaught exception propagates</li>
 * </ol>
 *
 * <p>A non-zero exit code causes Kubernetes to mark the Job as failed and,
 * depending on {@code backoffLimit}, retry it.</p>
 *
 * <p>Retention window: records whose {@code expires_at} is older than
 * {@code otp.cleanup.retention-hours} (default 24 h) are hard-deleted.
 * The buffer preserves a short audit window for incident investigation
 * before sensitive OTP hashes are permanently removed.</p>
 */
@Slf4j
@Component
@Profile("cleanup")
@RequiredArgsConstructor
public class OtpCleanupJob implements ApplicationRunner {

    private final OtpVerificationRepository otpRepo;

    /** How many hours past expiry before a record is eligible for hard deletion. */
    @Value("${otp.cleanup.retention-hours:24}")
    private int retentionHours;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(retentionHours);

        log.info("OTP cleanup starting — cutoff=[{}] retentionHours=[{}]", cutoff, retentionHours);

        long startMs = System.currentTimeMillis();
        int deleted = otpRepo.deleteExpiredBefore(cutoff);
        long elapsedMs = System.currentTimeMillis() - startMs;

        log.info("OTP cleanup completed — deleted=[{}] durationMs=[{}] cutoff=[{}]",
                deleted, elapsedMs, cutoff);
    }
}
