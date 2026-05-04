package com.shop.auth.service.impl;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.shop.auth.dto.ForgotPasswordRequestDto;
import com.shop.auth.dto.ResetPasswordRequestDto;
import com.shop.auth.entity.User;
import com.shop.auth.exception.PasswordHistoryViolationException;
import com.shop.auth.exception.PasswordResetTokenException;
import com.shop.auth.repository.UserRepository;
import com.shop.auth.service.EmailService;
import com.shop.auth.service.PasswordPolicyService;
import com.shop.auth.service.TokenBlacklistService;
import com.shop.auth.utils.HashUtil;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AuthServiceImpl — forgotPassword / resetPassword")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class AuthServiceImplForgotResetPasswordTest {

    @Mock private UserRepository                 userRepository;
    @Mock private PasswordEncoder                passwordEncoder;
    @Mock private PasswordPolicyService          passwordPolicyService;
    @Mock private TokenBlacklistService          tokenBlacklistService;
    @Mock private EmailService                   emailService;
    @Mock private StringRedisTemplate            redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks private AuthServiceImpl authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("John Doe");
        user.setEmail("john.doe@example.com");
        user.setPassword("$2a$10$existingHash");

        ReflectionTestUtils.setField(authService, "passwordResetBaseUrl",
                "http://localhost:3000/reset-password");
        ReflectionTestUtils.setField(authService, "passwordResetTokenTtlMinutes", 15);
        ReflectionTestUtils.setField(authService, "refreshTokenExpiryMs", 604800000L);
    }

    private ForgotPasswordRequestDto forgotRequest(String email) {
        ForgotPasswordRequestDto dto = new ForgotPasswordRequestDto();
        dto.setEmail(email);
        return dto;
    }

    // ── forgotPassword ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("forgotPassword")
    class ForgotPassword {

        @Test
        @DisplayName("Should store hashed reset token in Redis with 15-minute TTL when user is found")
        void shouldStoreHashedTokenInRedis() {
            when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(user));
            when(redisTemplate.opsForValue()).thenReturn(valueOps);

            authService.forgotPassword(forgotRequest("john.doe@example.com"));

            ArgumentCaptor<String> keyCaptor   = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
            verify(valueOps).set(keyCaptor.capture(), valueCaptor.capture(),
                    eq(900L), eq(TimeUnit.SECONDS));

            assertThat(keyCaptor.getValue()).startsWith("reset:token:");
            assertThat(valueCaptor.getValue()).isEqualTo("1"); // user.getId() as String
        }

        @Test
        @DisplayName("Should send password reset email with a link containing the raw token")
        void shouldSendResetEmailWithResetLink() {
            when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(user));
            when(redisTemplate.opsForValue()).thenReturn(valueOps);

            authService.forgotPassword(forgotRequest("john.doe@example.com"));

            verify(emailService).sendPasswordResetEmail(
                    eq("john.doe@example.com"),
                    eq("John Doe"),
                    argThat(link -> link.startsWith("http://localhost:3000/reset-password?token=")),
                    eq(15));
        }

        @Test
        @DisplayName("Should return normally without exception when email is not registered — prevents enumeration")
        void shouldNotThrowForUnknownEmail() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatNoException().isThrownBy(
                    () -> authService.forgotPassword(forgotRequest("unknown@example.com")));
        }

        @Test
        @DisplayName("Should never write to Redis or send email when user is not found — no enumeration")
        void shouldNotSendEmailOrWriteRedisForUnknownEmail() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            authService.forgotPassword(forgotRequest("unknown@example.com"));

            verify(redisTemplate, never()).opsForValue();
            verify(emailService, never()).sendPasswordResetEmail(any(), any(), any(), anyInt());
        }

        @Test
        @DisplayName("Should complete normally when email sending fails — prevents denial of service on SMTP errors")
        void shouldCompleteNormallyWhenEmailThrows() {
            when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(user));
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            doThrow(new RuntimeException("SMTP connection refused"))
                    .when(emailService).sendPasswordResetEmail(any(), any(), any(), anyInt());

            assertThatNoException().isThrownBy(
                    () -> authService.forgotPassword(forgotRequest("john.doe@example.com")));
        }
    }

    // ── resetPassword ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        private static final String RAW_TOKEN   = "test-raw-reset-token";
        private static final String TOKEN_HASH  = HashUtil.sha256Hex(RAW_TOKEN);
        private static final String REDIS_KEY   = "reset:token:" + TOKEN_HASH;

        private ResetPasswordRequestDto buildRequest(String token, String newPassword) {
            ResetPasswordRequestDto dto = new ResetPasswordRequestDto();
            dto.setToken(token);
            dto.setNewPassword(newPassword);
            return dto;
        }

        @Test
        @DisplayName("Should throw PasswordResetTokenException when token is not found in Redis")
        void shouldThrowWhenTokenNotFound() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(REDIS_KEY)).thenReturn(null);

            assertThatThrownBy(() -> authService.resetPassword(buildRequest(RAW_TOKEN, "NewPass@456")))
                    .isInstanceOf(PasswordResetTokenException.class);
        }

        @Test
        @DisplayName("Should throw PasswordResetTokenException when stored user ID has no matching user")
        void shouldThrowWhenUserNotFound() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(REDIS_KEY)).thenReturn("99");
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resetPassword(buildRequest(RAW_TOKEN, "NewPass@456")))
                    .isInstanceOf(PasswordResetTokenException.class);
        }

        @Test
        @DisplayName("Should delete the token from Redis on success — single-use enforced")
        void shouldDeleteTokenOnSuccess() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(REDIS_KEY)).thenReturn("1");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("NewPass@456")).thenReturn("$2a$10$newHash");
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authService.resetPassword(buildRequest(RAW_TOKEN, "NewPass@456"));

            verify(redisTemplate).delete(REDIS_KEY);
        }

        @Test
        @DisplayName("Should save user with new encoded password and updated passwordChangedAt")
        void shouldSaveUserWithNewPassword() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(REDIS_KEY)).thenReturn("1");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("NewPass@456")).thenReturn("$2a$10$newHash");
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authService.resetPassword(buildRequest(RAW_TOKEN, "NewPass@456"));

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getPassword()).isEqualTo("$2a$10$newHash");
            assertThat(captor.getValue().getPasswordChangedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should invalidate all active sessions after a successful password reset")
        void shouldInvalidateAllSessionsOnSuccess() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(REDIS_KEY)).thenReturn("1");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("NewPass@456")).thenReturn("$2a$10$newHash");
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authService.resetPassword(buildRequest(RAW_TOKEN, "NewPass@456"));

            verify(tokenBlacklistService).invalidateAllUserTokens(eq(1L), anyLong());
        }

        @Test
        @DisplayName("Should NOT delete token when history check fails — token stays valid for retry")
        void shouldNotConsumeTokenOnHistoryViolation() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(REDIS_KEY)).thenReturn("1");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            doThrow(new PasswordHistoryViolationException(12))
                    .when(passwordPolicyService).enforceHistory(eq(user), eq("NewPass@456"));

            assertThatThrownBy(() -> authService.resetPassword(buildRequest(RAW_TOKEN, "NewPass@456")))
                    .isInstanceOf(PasswordHistoryViolationException.class);

            verify(redisTemplate, never()).delete(anyString());
        }
    }
}
