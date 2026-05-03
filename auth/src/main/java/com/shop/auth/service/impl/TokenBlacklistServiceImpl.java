package com.shop.auth.service.impl;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.shop.auth.service.TokenBlacklistService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Stores revoked JWT IDs in Redis with a TTL equal to the token's remaining lifetime.
 *
 * <p>Key format: {@code blacklist:jti:<jti>} — value is always {@code "1"}.
 * Existence of the key is the revocation signal; the value is irrelevant.</p>
 *
 * <p>Redis O(1) SET / EXISTS operations make this negligible per-request overhead.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private static final String PREFIX = "blacklist:jti:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void blacklist(String jti, long ttlSeconds) {
        if (ttlSeconds <= 0) {
            log.debug("Token jti=[{}] already expired — skipping blacklist entry", jti);
            return;
        }
        redisTemplate.opsForValue().set(PREFIX + jti, "1", ttlSeconds, TimeUnit.SECONDS);
        log.debug("Token blacklisted: jti=[{}] ttl=[{}s]", jti, ttlSeconds);
    }

    @Override
    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + jti));
    }
}
