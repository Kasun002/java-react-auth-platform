package com.org.auth.service.impl;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("LoginRateLimitServiceImpl")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class LoginRateLimitServiceImplTest {

    @Mock private StringRedisTemplate            redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks private LoginRateLimitServiceImpl rateLimitService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(rateLimitService, "maxAttempts", 10);
        ReflectionTestUtils.setField(rateLimitService, "windowMinutes", 15);
    }

    // ── First attempt — expiry is set ─────────────────────────────────────────

    @Nested
    @DisplayName("First attempt in window")
    class FirstAttempt {

        @Test
        @DisplayName("Should allow and set TTL on first attempt (count == 1)")
        void shouldAllowAndSetTtlOnFirstAttempt() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.increment("login:rate:ip:192.168.1.1")).thenReturn(1L);

            boolean result = rateLimitService.checkAndIncrement("192.168.1.1");

            assertThat(result).isTrue();
            verify(redisTemplate).expire("login:rate:ip:192.168.1.1", 15L, TimeUnit.MINUTES);
        }
    }

    // ── Subsequent attempts within limit ──────────────────────────────────────

    @Nested
    @DisplayName("Subsequent attempts within limit")
    class WithinLimit {

        @Test
        @DisplayName("Should allow when count equals maxAttempts exactly")
        void shouldAllowWhenCountEqualsMax() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.increment("login:rate:ip:10.0.0.1")).thenReturn(10L);

            assertThat(rateLimitService.checkAndIncrement("10.0.0.1")).isTrue();
            verify(redisTemplate, never()).expire(any(), anyLong(), any());
        }
    }

    // ── Limit exceeded ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Limit exceeded")
    class LimitExceeded {

        @Test
        @DisplayName("Should block when count exceeds maxAttempts")
        void shouldBlockWhenCountExceedsMax() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.increment("login:rate:ip:10.0.0.1")).thenReturn(11L);

            assertThat(rateLimitService.checkAndIncrement("10.0.0.1")).isFalse();
        }
    }

    // ── Redis failure — fail-secure ───────────────────────────────────────────

    @Nested
    @DisplayName("Redis failure (fail-secure)")
    class RedisFailure {

        @Test
        @DisplayName("Should block when Redis INCR returns null")
        void shouldBlockWhenRedisReturnsNull() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.increment("login:rate:ip:bad-ip")).thenReturn(null);

            assertThat(rateLimitService.checkAndIncrement("bad-ip")).isFalse();
            verify(redisTemplate, never()).expire(any(), anyLong(), any());
        }
    }
}
