package com.org.auth.service.impl;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.org.auth.service.LoginRateLimitService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis-backed login rate limiter using atomic INCR + EXPIRE.
 *
 * <p>
 * Key format: {@code login:rate:ip:<ipAddress>}.
 * The key is set to expire after the configured window, resetting the counter.
 * </p>
 *
 * <p>
 * If Redis is unavailable (INCR returns null), the attempt is blocked
 * (fail-secure) to prevent brute-force attacks from bypassing the limit.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginRateLimitServiceImpl implements LoginRateLimitService {

    private static final String PREFIX = "login:rate:ip:";

    private final StringRedisTemplate redisTemplate;

    @Value("${app.security.login.max-attempts:10}")
    private int maxAttempts;

    @Value("${app.security.login.window-minutes:15}")
    private int windowMinutes;

    @Override
    public boolean checkAndIncrement(String ipAddress) {
        String key = PREFIX + ipAddress;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            log.warn("Redis INCR returned null for login rate key=[{}] — blocking attempt (fail-secure)", key);
            return false;
        }
        if (count == 1L) {
            redisTemplate.expire(key, windowMinutes, TimeUnit.MINUTES);
        }
        boolean allowed = count <= maxAttempts;
        if (!allowed) {
            log.warn("Login rate limit exceeded for ip=[{}] count=[{}] max=[{}]", ipAddress, count, maxAttempts);
        }
        return allowed;
    }
}
