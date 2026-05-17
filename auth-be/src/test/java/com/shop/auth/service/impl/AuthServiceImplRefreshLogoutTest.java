package com.shop.auth.service.impl;

import java.util.Date;
import java.util.Optional;

import com.shop.auth.dto.RefreshTokenRequestDto;
import com.shop.auth.dto.RefreshTokenResponseDto;
import com.shop.auth.entity.User;
import com.shop.auth.exception.InvalidTokenException;
import com.shop.auth.repository.UserLogRepository;
import com.shop.auth.repository.UserRepository;
import com.shop.auth.service.JwtService;
import com.shop.auth.service.TokenBlacklistService;
import com.shop.auth.utils.TokenType;
import com.shop.auth.utils.UserStatus;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AuthServiceImpl — refresh / logout")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class AuthServiceImplRefreshLogoutTest {

    @Mock private UserRepository        userRepository;
    @Mock private UserLogRepository     userLogRepository;
    @Mock private JwtService            jwtService;
    @Mock private TokenBlacklistService tokenBlacklistService;

    @InjectMocks private AuthServiceImpl authService;

    private static final String REFRESH_TOKEN = "valid.refresh.token";
    private static final String ACCESS_TOKEN  = "valid.access.token";

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = new User();
        activeUser.setId(1L);
        activeUser.setEmail("john.doe@example.com");
        activeUser.setStatus(UserStatus.ACTIVE);
    }

    // ── refresh ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("refresh — validation failures")
    class RefreshValidationFailures {

        private RefreshTokenRequestDto request(String token) {
            RefreshTokenRequestDto dto = new RefreshTokenRequestDto();
            dto.setRefreshToken(token);
            return dto;
        }

        @Test
        @DisplayName("Should throw InvalidTokenException when token signature or expiry is invalid — Step 1")
        void shouldThrowOnInvalidToken() {
            when(jwtService.isTokenValid(REFRESH_TOKEN)).thenReturn(false);

            assertThatThrownBy(() -> authService.refresh(request(REFRESH_TOKEN)))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("Should throw InvalidTokenException when token type is ACCESS, not REFRESH — Step 2")
        void shouldThrowWhenWrongTokenType() {
            when(jwtService.isTokenValid(REFRESH_TOKEN)).thenReturn(true);
            when(jwtService.extractTokenType(REFRESH_TOKEN)).thenReturn(TokenType.ACCESS.name());

            assertThatThrownBy(() -> authService.refresh(request(REFRESH_TOKEN)))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("Should throw InvalidTokenException when refresh token JTI is blacklisted — Step 3")
        void shouldThrowWhenTokenBlacklisted() {
            when(jwtService.isTokenValid(REFRESH_TOKEN)).thenReturn(true);
            when(jwtService.extractTokenType(REFRESH_TOKEN)).thenReturn(TokenType.REFRESH.name());
            when(jwtService.extractJti(REFRESH_TOKEN)).thenReturn("refresh-jti");
            when(tokenBlacklistService.isBlacklisted("refresh-jti")).thenReturn(true);

            assertThatThrownBy(() -> authService.refresh(request(REFRESH_TOKEN)))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("Should throw InvalidTokenException when user account does not exist — Step 4")
        void shouldThrowWhenUserNotFound() {
            when(jwtService.isTokenValid(REFRESH_TOKEN)).thenReturn(true);
            when(jwtService.extractTokenType(REFRESH_TOKEN)).thenReturn(TokenType.REFRESH.name());
            when(jwtService.extractJti(REFRESH_TOKEN)).thenReturn("refresh-jti");
            when(tokenBlacklistService.isBlacklisted("refresh-jti")).thenReturn(false);
            when(jwtService.extractUsername(REFRESH_TOKEN)).thenReturn("john.doe@example.com");
            when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh(request(REFRESH_TOKEN)))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("Should throw InvalidTokenException when user account is not ACTIVE — Step 4")
        void shouldThrowWhenUserNotActive() {
            activeUser.setStatus(UserStatus.INACTIVE);
            when(jwtService.isTokenValid(REFRESH_TOKEN)).thenReturn(true);
            when(jwtService.extractTokenType(REFRESH_TOKEN)).thenReturn(TokenType.REFRESH.name());
            when(jwtService.extractJti(REFRESH_TOKEN)).thenReturn("refresh-jti");
            when(tokenBlacklistService.isBlacklisted("refresh-jti")).thenReturn(false);
            when(jwtService.extractUsername(REFRESH_TOKEN)).thenReturn("john.doe@example.com");
            when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(activeUser));

            assertThatThrownBy(() -> authService.refresh(request(REFRESH_TOKEN)))
                    .isInstanceOf(InvalidTokenException.class);
        }
    }

    @Nested
    @DisplayName("refresh — successful token rotation")
    class RefreshSuccess {

        private final Date futureDate = new Date(System.currentTimeMillis() + 900_000);

        @BeforeEach
        void stubValidRefreshPath() {
            when(jwtService.isTokenValid(REFRESH_TOKEN)).thenReturn(true);
            when(jwtService.extractTokenType(REFRESH_TOKEN)).thenReturn(TokenType.REFRESH.name());
            when(jwtService.extractJti(REFRESH_TOKEN)).thenReturn("refresh-jti");
            when(tokenBlacklistService.isBlacklisted("refresh-jti")).thenReturn(false);
            when(jwtService.extractUsername(REFRESH_TOKEN)).thenReturn("john.doe@example.com");
            when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(activeUser));
            when(jwtService.generateAccessToken(activeUser)).thenReturn("new.access.token");
            when(jwtService.generateRefreshToken(activeUser)).thenReturn("new.refresh.token");
            when(jwtService.extractExpiration(anyString())).thenReturn(futureDate);
            when(userLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        }

        private RefreshTokenRequestDto request() {
            RefreshTokenRequestDto dto = new RefreshTokenRequestDto();
            dto.setRefreshToken(REFRESH_TOKEN);
            return dto;
        }

        @Test
        @DisplayName("Should return a new access token and a new refresh token")
        void shouldReturnNewTokenPair() {
            RefreshTokenResponseDto result = authService.refresh(request());

            assertThat(result.getAccessToken()).isEqualTo("new.access.token");
            assertThat(result.getRefreshToken()).isEqualTo("new.refresh.token");
        }

        @Test
        @DisplayName("Should blacklist the consumed refresh token — single-use enforced")
        void shouldBlacklistConsumedRefreshToken() {
            authService.refresh(request());

            verify(tokenBlacklistService).blacklist(eq("refresh-jti"), anyLong());
        }

        @Test
        @DisplayName("Should persist two user log entries — one for the new ACCESS token, one for REFRESH")
        void shouldPersistTwoUserLogEntries() {
            authService.refresh(request());

            verify(userLogRepository, times(2)).save(any());
        }
    }

    // ── logout ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("logout")
    class LogoutTests {

        @Test
        @DisplayName("Should blacklist both access and refresh token JTIs on full logout")
        void shouldBlacklistBothTokens() {
            when(jwtService.extractUsername(ACCESS_TOKEN)).thenReturn("john.doe@example.com");
            when(jwtService.extractJti(ACCESS_TOKEN)).thenReturn("access-jti");
            when(jwtService.extractExpiration(ACCESS_TOKEN))
                    .thenReturn(new Date(System.currentTimeMillis() + 900_000));
            when(jwtService.extractJti(REFRESH_TOKEN)).thenReturn("refresh-jti-2");
            when(jwtService.extractExpiration(REFRESH_TOKEN))
                    .thenReturn(new Date(System.currentTimeMillis() + 604_800_000));

            authService.logout(ACCESS_TOKEN, REFRESH_TOKEN);

            verify(tokenBlacklistService).blacklist(eq("access-jti"), anyLong());
            verify(tokenBlacklistService).blacklist(eq("refresh-jti-2"), anyLong());
        }

        @Test
        @DisplayName("Should blacklist only the access token when refresh token is null")
        void shouldBlacklistOnlyAccessTokenWhenRefreshIsNull() {
            when(jwtService.extractUsername(ACCESS_TOKEN)).thenReturn("john.doe@example.com");
            when(jwtService.extractJti(ACCESS_TOKEN)).thenReturn("access-jti");
            when(jwtService.extractExpiration(ACCESS_TOKEN))
                    .thenReturn(new Date(System.currentTimeMillis() + 900_000));

            authService.logout(ACCESS_TOKEN, null);

            // Only one blacklist call — the refresh token is not revoked
            verify(tokenBlacklistService, times(1)).blacklist(anyString(), anyLong());
        }

        @Test
        @DisplayName("Should blacklist only the access token when refresh token is blank")
        void shouldBlacklistOnlyAccessTokenWhenRefreshIsBlank() {
            when(jwtService.extractUsername(ACCESS_TOKEN)).thenReturn("john.doe@example.com");
            when(jwtService.extractJti(ACCESS_TOKEN)).thenReturn("access-jti");
            when(jwtService.extractExpiration(ACCESS_TOKEN))
                    .thenReturn(new Date(System.currentTimeMillis() + 900_000));

            authService.logout(ACCESS_TOKEN, "   ");

            verify(tokenBlacklistService, times(1)).blacklist(anyString(), anyLong());
        }

        @Test
        @DisplayName("Should complete normally even when JTI extraction throws — revokeToken swallows exceptions")
        void shouldCompleteNormallyWhenExtractionFails() {
            when(jwtService.extractUsername(ACCESS_TOKEN)).thenReturn("john.doe@example.com");
            when(jwtService.extractJti(ACCESS_TOKEN)).thenThrow(new RuntimeException("parse error"));

            assertThatNoException().isThrownBy(() -> authService.logout(ACCESS_TOKEN, null));
        }
    }
}
