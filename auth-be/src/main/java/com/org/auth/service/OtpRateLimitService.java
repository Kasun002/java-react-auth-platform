package com.org.auth.service;

public interface OtpRateLimitService {

    /**
     * Atomically increments the OTP resend counter for the given user and checks
     * whether the new count is within the allowed limit.
     *
     * <p>The counter key expires after one hour from the first increment in the window,
     * at which point the window resets automatically.</p>
     *
     * @param userId     the user whose resend counter to increment
     * @param maxPerHour the maximum number of resends allowed per hour
     * @return {@code true} if the resend is permitted; {@code false} if the limit is exceeded
     */
    boolean checkAndIncrementResend(Long userId, int maxPerHour);
}
