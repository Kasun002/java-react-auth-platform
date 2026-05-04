package com.shop.auth.service.impl;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("TokenBlacklistServiceImpl")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class TokenBlacklistServiceImplTest {

    @Mock private StringRedisTemplate            redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks private TokenBlacklistServiceImpl tokenBlacklistService;

    // ── Per-token blacklist — blacklist() ─────────────────────────────────────

    @Nested
    @DisplayName("blacklist(jti, ttl)")
    class BlacklistMethod {

        @Test
        @DisplayName("Should write 'blacklist:jti:<jti>' key to Redis with the given TTL")
        void shouldWriteToRedisWhenTtlPositive() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);

            tokenBlacklistService.blacklist("my-jti", 300L);

            verify(valueOps).set("blacklist:jti:my-jti", "1", 300L, TimeUnit.SECONDS);
        }

        @Test
        @DisplayName("Should skip Redis write when TTL is zero — token already expired")
        void shouldSkipWhenTtlIsZero() {
            tokenBlacklistService.blacklist("my-jti", 0L);

            verify(redisTemplate, never()).opsForValue();
        }

        @Test
        @DisplayName("Should skip Redis write when TTL is negative")
        void shouldSkipWhenTtlIsNegative() {
            tokenBlacklistService.blacklist("my-jti", -1L);

            verify(redisTemplate, never()).opsForValue();
        }
    }

    // ── Per-token blacklist — isBlacklisted() ─────────────────────────────────

    @Nested
    @DisplayName("isBlacklisted(jti)")
    class IsBlacklisted {

        @Test
        @DisplayName("Should return true when the JTI key exists in Redis")
        void shouldReturnTrueWhenKeyExists() {
            when(redisTemplate.hasKey("blacklist:jti:revoked-jti")).thenReturn(true);

            assertThat(tokenBlacklistService.isBlacklisted("revoked-jti")).isTrue();
        }

        @Test
        @DisplayName("Should return false when the JTI key is absent from Redis")
        void shouldReturnFalseWhenKeyAbsent() {
            when(redisTemplate.hasKey("blacklist:jti:valid-jti")).thenReturn(false);

            assertThat(tokenBlacklistService.isBlacklisted("valid-jti")).isFalse();
        }

        @Test
        @DisplayName("Should return false when Redis returns null — safe against null Boolean")
        void shouldReturnFalseWhenRedisReturnsNull() {
            when(redisTemplate.hasKey("blacklist:jti:any-jti")).thenReturn(null);

            assertThat(tokenBlacklistService.isBlacklisted("any-jti")).isFalse();
        }
    }

    // ── User-level invalidation — invalidateAllUserTokens() ───────────────────

    @Nested
    @DisplayName("invalidateAllUserTokens(userId, ttl)")
    class InvalidateAllUserTokens {

        @Test
        @DisplayName("Should write current epoch seconds to 'user:tokens:invalidated:<userId>' with given TTL")
        void shouldWriteEpochToRedis() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);

            long before = Instant.now().getEpochSecond();
            tokenBlacklistService.invalidateAllUserTokens(42L, 604800L);
            long after = Instant.now().getEpochSecond();

            ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
            verify(valueOps).set(eq("user:tokens:invalidated:42"), valueCaptor.capture(),
                    eq(604800L), eq(TimeUnit.SECONDS));

            long storedEpoch = Long.parseLong(valueCaptor.getValue());
            assertThat(storedEpoch).isBetween(before, after);
        }

        @Test
        @DisplayName("Should skip Redis write when TTL is zero")
        void shouldSkipWhenTtlIsZero() {
            tokenBlacklistService.invalidateAllUserTokens(42L, 0L);

            verify(redisTemplate, never()).opsForValue();
        }

        @Test
        @DisplayName("Should skip Redis write when TTL is negative")
        void shouldSkipWhenTtlIsNegative() {
            tokenBlacklistService.invalidateAllUserTokens(42L, -10L);

            verify(redisTemplate, never()).opsForValue();
        }
    }

    // ── User-level invalidation — isUserTokensInvalidated() ───────────────────

    @Nested
    @DisplayName("isUserTokensInvalidated(userId, tokenIssuedAt)")
    class IsUserTokensInvalidated {

        @BeforeEach
        void stubOpsForValue() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
        }

        @Test
        @DisplayName("Should return false when no invalidation record exists for the user")
        void shouldReturnFalseWhenNoRecord() {
            when(valueOps.get("user:tokens:invalidated:42")).thenReturn(null);

            assertThat(tokenBlacklistService.isUserTokensInvalidated(42L, Instant.now())).isFalse();
        }

        @Test
        @DisplayName("Should return true when token was issued before the invalidation event")
        void shouldReturnTrueWhenTokenIsOlderThanInvalidation() {
            Instant invalidatedAt = Instant.now();
            Instant tokenIssuedAt = invalidatedAt.minusSeconds(60); // issued 60s before invalidation

            when(valueOps.get("user:tokens:invalidated:42"))
                    .thenReturn(String.valueOf(invalidatedAt.getEpochSecond()));

            assertThat(tokenBlacklistService.isUserTokensInvalidated(42L, tokenIssuedAt)).isTrue();
        }

        @Test
        @DisplayName("Should return false when token was issued after the invalidation event")
        void shouldReturnFalseWhenTokenIsNewerThanInvalidation() {
            Instant invalidatedAt = Instant.now().minusSeconds(120);
            Instant tokenIssuedAt = invalidatedAt.plusSeconds(60); // issued 60s after invalidation

            when(valueOps.get("user:tokens:invalidated:42"))
                    .thenReturn(String.valueOf(invalidatedAt.getEpochSecond()));

            assertThat(tokenBlacklistService.isUserTokensInvalidated(42L, tokenIssuedAt)).isFalse();
        }

        @Test
        @DisplayName("Should return false when token was issued at exactly the same second as invalidation")
        void shouldReturnFalseWhenTokenIssuedAtSameSecond() {
            Instant sameInstant = Instant.now();

            when(valueOps.get("user:tokens:invalidated:99"))
                    .thenReturn(String.valueOf(sameInstant.getEpochSecond()));

            // epochSecond of token == invalidatedAtEpoch → not strictly less → returns false
            assertThat(tokenBlacklistService.isUserTokensInvalidated(99L, sameInstant)).isFalse();
        }
    }
}
