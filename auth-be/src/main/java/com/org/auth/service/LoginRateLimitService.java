package com.org.auth.service;

/**
 * Rate-limits login attempts per client IP address.
 *
 * <p>Returns {@code true} when the attempt is within the allowed window, {@code false} (or throws)
 * when the limit is exceeded.</p>
 */
public interface LoginRateLimitService {

    /**
     * Increments the attempt counter for the given IP and checks whether it is
     * still within the configured limit.
     *
     * @param ipAddress the client IP address
     * @return {@code true} if the attempt is allowed; {@code false} if the limit is exceeded
     */
    boolean checkAndIncrement(String ipAddress);
}
