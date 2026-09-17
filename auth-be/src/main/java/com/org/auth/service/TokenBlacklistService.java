package com.org.auth.service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis-backed token revocation store.
 *
 * <p>
 * Two revocation mechanisms:
 * </p>
 * <ol>
 * <li><b>Per-token blacklist</b> — key {@code blacklist:jti:<jti>}, value
 * {@code "1"}, TTL = remaining token lifetime.
 * Used for individual logout and refresh token rotation.</li>
 * <li><b>User-level invalidation</b> — key
 * {@code user:tokens:invalidated:<userId>}, value = epoch seconds of
 * invalidation,
 * TTL = refresh token lifetime. Used on password change, account suspension,
 * and admin-forced logout.
 * Any token whose {@code iat} is older than this timestamp is rejected.</li>
 * </ol>
 *
 * <p>
 * All Redis operations are O(1) — negligible per-request overhead.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String JTI_PREFIX = "blacklist:jti:";
    private static final String USER_PREFIX = "user:tokens:invalidated:";

    private final StringRedisTemplate redisTemplate;

    // ── Per-token revocation ──────────────────────────────────────────────────

    public void blacklist(String jti, long ttlSeconds) {
        if (ttlSeconds <= 0) {
            log.debug("Token jti=[{}] already expired — skipping blacklist entry", jti);
            return;
        }
        redisTemplate.opsForValue().set(JTI_PREFIX + jti, "1", ttlSeconds, TimeUnit.SECONDS);
        log.debug("Token blacklisted: jti=[{}] ttl=[{}s]", jti, ttlSeconds);
    }

    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(JTI_PREFIX + jti));
    }

    // ── User-level revocation ─────────────────────────────────────────────────

    public void invalidateAllUserTokens(Long userId, long ttlSeconds) {
        if (ttlSeconds <= 0)
            return;
        String epochNow = String.valueOf(Instant.now().getEpochSecond());
        redisTemplate.opsForValue().set(USER_PREFIX + userId, epochNow, ttlSeconds, TimeUnit.SECONDS);
        log.debug("All tokens invalidated for userId=[{}] ttl=[{}s]", userId, ttlSeconds);
    }

    public boolean isUserTokensInvalidated(Long userId, Instant tokenIssuedAt) {
        String raw = redisTemplate.opsForValue().get(USER_PREFIX + userId);
        if (raw == null)
            return false;
        long invalidatedAtEpoch;
        try {
            invalidatedAtEpoch = Long.parseLong(raw);
        } catch (NumberFormatException e) {
            log.error("Corrupt user invalidation record for userId=[{}] value=[{}] — treating as not invalidated",
                    userId, raw);
            return false;
        }
        return tokenIssuedAt.getEpochSecond() < invalidatedAtEpoch;
    }
}
