package com.shop.auth.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import com.shop.auth.entity.OtpVerification;
import com.shop.auth.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    /**
     * Returns the most recent unused OTP for the given user.
     * Acquires a pessimistic write lock to prevent concurrent verify requests from
     * racing past the attempt counter check (banking requirement: atomic attempt tracking).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OtpVerification> findTopByUserAndUsedFalseOrderByCreatedAtDesc(User user);

    /** Counts all OTP records created for the user after a given timestamp — used for resend rate-limiting. */
    long countByUserAndCreatedAtAfter(User user, LocalDateTime after);

    /**
     * Marks all current unused OTP records for the user as consumed.
     * Called before issuing a new OTP so that previously captured codes are invalidated.
     */
    @Modifying
    @Query("UPDATE OtpVerification o SET o.used = true WHERE o.user = :user AND o.used = false")
    void invalidateAllUnusedForUser(@Param("user") User user);

    /**
     * Bulk-deletes all OTP records whose expiry timestamp is before the given cutoff.
     *
     * <p>Called exclusively by the OTP cleanup CronJob (cleanup Spring profile).
     * The cutoff is typically {@code NOW() - retentionHours} so that a short audit
     * window is preserved before hard deletion.</p>
     *
     * @param cutoff records with {@code expiresAt} before this timestamp are deleted
     * @return number of rows deleted
     */
    @Modifying
    @Query("DELETE FROM OtpVerification o WHERE o.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);
}
