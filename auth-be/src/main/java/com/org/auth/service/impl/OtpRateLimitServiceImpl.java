package com.org.auth.service.impl;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.org.auth.service.OtpRateLimitService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis-backed OTP resend rate limiter using atomic INCR + EXPIRE.
 *
 * <p>
 * Key format: {@code otp:resend:<userId>}.
 * The key is created on the first resend and expires 1 hour later, resetting
 * the window.
 * </p>
 *
 * <p>
 * INCR is atomic — no race condition between the increment and the limit check.
 * If the incremented value exceeds the cap, the resend is rejected (the counter
 * continues to accumulate, which is intentional: further attempts within the
 * window
 * remain blocked).
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpRateLimitServiceImpl implements OtpRateLimitService {

    private static final String PREFIX = "otp:resend:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean checkAndIncrementResend(Long userId, int maxPerHour) {
        String key = PREFIX + userId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            log.warn("Redis INCR returned null for key=[{}] — allowing resend as safe fallback", key);
            return true;
        }
        if (count == 1L) {
            // First resend in this window — set the 1-hour expiry
            redisTemplate.expire(key, 1, TimeUnit.HOURS);
        }
        boolean allowed = count <= maxPerHour;
        log.debug("OTP resend rate check: userId=[{}] count=[{}] max=[{}] allowed=[{}]",
                userId, count, maxPerHour, allowed);
        return allowed;
    }
}
