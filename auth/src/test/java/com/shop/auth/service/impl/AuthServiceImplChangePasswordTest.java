package com.shop.auth.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;

import com.shop.auth.dto.ChangePasswordRequestDto;
import com.shop.auth.entity.User;
import com.shop.auth.exception.InvalidCredentialsException;
import com.shop.auth.exception.PasswordHistoryViolationException;
import com.shop.auth.repository.UserLogRepository;
import com.shop.auth.repository.UserRepository;
import com.shop.auth.service.JwtService;
import com.shop.auth.service.PasswordPolicyService;
import com.shop.auth.service.TokenBlacklistService;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AuthServiceImpl — changePassword")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class AuthServiceImplChangePasswordTest {

    @Mock private UserRepository        userRepository;
    @Mock private UserLogRepository     userLogRepository;
    @Mock private JwtService            jwtService;
    @Mock private PasswordEncoder       passwordEncoder;
    @Mock private PasswordPolicyService passwordPolicyService;
    @Mock private TokenBlacklistService tokenBlacklistService;

    @InjectMocks private AuthServiceImpl authService;

    private static final String ACCESS_TOKEN = "valid.access.token";

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("john.doe@example.com");
        user.setPassword("$2a$10$currentHash");

        ReflectionTestUtils.setField(authService, "refreshTokenExpiryMs", 604800000L);
    }

    private ChangePasswordRequestDto buildRequest(String current, String newPass) {
        ChangePasswordRequestDto dto = new ChangePasswordRequestDto();
        dto.setCurrentPassword(current);
        dto.setNewPassword(newPass);
        return dto;
    }

    /** Stubs the common happy-path flow. */
    private void stubHappyPath() {
        when(jwtService.extractUserId(ACCESS_TOKEN)).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass@123", user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("NewPass@456")).thenReturn("$2a$10$newHash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ── Successful password change ────────────────────────────────────────────

    @Nested
    @DisplayName("Successful password change")
    class Success {

        @Test
        @DisplayName("Should complete without exception on a valid request")
        void shouldCompleteWithoutException() {
            stubHappyPath();

            assertThatNoException().isThrownBy(
                    () -> authService.changePassword(ACCESS_TOKEN, buildRequest("OldPass@123", "NewPass@456")));
        }

        @Test
        @DisplayName("Should save the user with the new encoded password")
        void shouldSaveNewEncodedPassword() {
            stubHappyPath();

            authService.changePassword(ACCESS_TOKEN, buildRequest("OldPass@123", "NewPass@456"));

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getPassword()).isEqualTo("$2a$10$newHash");
        }

        @Test
        @DisplayName("Should update passwordChangedAt to now when password is changed")
        void shouldUpdatePasswordChangedAt() {
            stubHappyPath();

            authService.changePassword(ACCESS_TOKEN, buildRequest("OldPass@123", "NewPass@456"));

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getPasswordChangedAt())
                    .as("passwordChangedAt must be set on password change")
                    .isNotNull()
                    .isBeforeOrEqualTo(LocalDateTime.now());
        }

        @Test
        @DisplayName("Should invalidate all user tokens — forces re-login on all devices")
        void shouldInvalidateAllUserTokens() {
            stubHappyPath();

            authService.changePassword(ACCESS_TOKEN, buildRequest("OldPass@123", "NewPass@456"));

            // TTL is refreshTokenExpiryMs / 1000 = 604800000 / 1000 = 604800s
            verify(tokenBlacklistService).invalidateAllUserTokens(eq(1L), eq(604800L));
        }

        @Test
        @DisplayName("Should record the new password in history after saving")
        void shouldRecordPasswordInHistory() {
            stubHappyPath();

            authService.changePassword(ACCESS_TOKEN, buildRequest("OldPass@123", "NewPass@456"));

            verify(passwordPolicyService).recordPasswordChange(eq(user), eq("$2a$10$newHash"));
        }
    }

    // ── Wrong current password ────────────────────────────────────────────────

    @Nested
    @DisplayName("Wrong current password")
    class WrongCurrentPassword {

        @Test
        @DisplayName("Should throw InvalidCredentialsException when current password does not match")
        void shouldThrowOnWrongCurrentPassword() {
            when(jwtService.extractUserId(ACCESS_TOKEN)).thenReturn(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("WrongPass", user.getPassword())).thenReturn(false);

            assertThatThrownBy(() -> authService.changePassword(
                    ACCESS_TOKEN, buildRequest("WrongPass", "NewPass@456")))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("Should never save user or invalidate tokens when current password is wrong")
        void shouldNeverSaveOrInvalidateOnWrongPassword() {
            when(jwtService.extractUserId(ACCESS_TOKEN)).thenReturn(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("WrongPass", user.getPassword())).thenReturn(false);

            assertThatThrownBy(() -> authService.changePassword(
                    ACCESS_TOKEN, buildRequest("WrongPass", "NewPass@456")))
                    .isInstanceOf(InvalidCredentialsException.class);

            verify(userRepository, never()).save(any());
            verify(tokenBlacklistService, never()).invalidateAllUserTokens(anyLong(), anyLong());
        }
    }

    // ── Password history enforcement ──────────────────────────────────────────

    @Nested
    @DisplayName("Password history enforcement")
    class HistoryEnforcement {

        @Test
        @DisplayName("Should throw PasswordHistoryViolationException when new password was recently used")
        void shouldThrowOnHistoryViolation() {
            when(jwtService.extractUserId(ACCESS_TOKEN)).thenReturn(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("OldPass@123", user.getPassword())).thenReturn(true);
            doThrow(new PasswordHistoryViolationException(12))
                    .when(passwordPolicyService).enforceHistory(eq(user), eq("NewPass@456"));

            assertThatThrownBy(() -> authService.changePassword(
                    ACCESS_TOKEN, buildRequest("OldPass@123", "NewPass@456")))
                    .isInstanceOf(PasswordHistoryViolationException.class)
                    .hasMessageContaining("last 12");
        }

        @Test
        @DisplayName("Should never encode or save password when history check fails")
        void shouldNeverSaveOnHistoryViolation() {
            when(jwtService.extractUserId(ACCESS_TOKEN)).thenReturn(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("OldPass@123", user.getPassword())).thenReturn(true);
            doThrow(new PasswordHistoryViolationException(12))
                    .when(passwordPolicyService).enforceHistory(eq(user), eq("NewPass@456"));

            assertThatThrownBy(() -> authService.changePassword(
                    ACCESS_TOKEN, buildRequest("OldPass@123", "NewPass@456")))
                    .isInstanceOf(PasswordHistoryViolationException.class);

            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepository, never()).save(any());
        }
    }
}
