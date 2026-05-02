package com.shop.auth.service.impl;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

import com.shop.auth.dto.LoginRequestDto;
import com.shop.auth.dto.LoginResponseDto;
import com.shop.auth.entity.User;
import com.shop.auth.exception.AccountLockedException;
import com.shop.auth.exception.InvalidCredentialsException;
import com.shop.auth.exception.UserNotActiveException;
import com.shop.auth.fixtures.LoginRequestDtoFixture;
import com.shop.auth.repository.UserLogRepository;
import com.shop.auth.repository.UserRepository;
import com.shop.auth.service.JwtService;
import com.shop.auth.utils.Role;
import com.shop.auth.utils.TokenType;
import com.shop.auth.utils.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AuthServiceImpl — login")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class AuthServiceImplLoginTest {

    @Mock private UserRepository    userRepository;
    @Mock private UserLogRepository userLogRepository;
    @Mock private PasswordEncoder   passwordEncoder;
    @Mock private JwtService        jwtService;
    @InjectMocks private AuthServiceImpl authService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = new User();
        activeUser.setId(1L);
        activeUser.setName("John Doe");
        activeUser.setEmail("john.doe@example.com");
        activeUser.setPassword("$2a$10$hashed");
        activeUser.setStatus(UserStatus.ACTIVE);
        activeUser.setRole(Role.USER);
    }

    /** Stubs for the full happy-path flow. */
    private void stubHappyPath(LoginRequestDto request) {
        when(userRepository.findByEmail(request.getUsername())).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(request.getPassword(), activeUser.getPassword())).thenReturn(true);
        when(jwtService.generateAccessToken(activeUser)).thenReturn("access.token.value");
        when(jwtService.generateRefreshToken(activeUser)).thenReturn("refresh.token.value");
        when(jwtService.extractExpiration(anyString())).thenReturn(new Date(System.currentTimeMillis() + 900_000));
        when(userLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ── Successful login ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Successful login")
    class Success {

        @Test
        @DisplayName("Should return access token on successful login")
        void shouldReturnAccessToken() {
            LoginRequestDto request = LoginRequestDtoFixture.valid();
            stubHappyPath(request);

            LoginResponseDto response = authService.login(request);

            assertThat(response.getAccessToken()).isEqualTo("access.token.value");
        }

        @Test
        @DisplayName("Should return refresh token on successful login")
        void shouldReturnRefreshToken() {
            LoginRequestDto request = LoginRequestDtoFixture.valid();
            stubHappyPath(request);

            LoginResponseDto response = authService.login(request);

            assertThat(response.getRefreshToken()).isEqualTo("refresh.token.value");
        }

        @Test
        @DisplayName("Should return user name in response")
        void shouldReturnUserName() {
            LoginRequestDto request = LoginRequestDtoFixture.valid();
            stubHappyPath(request);

            LoginResponseDto response = authService.login(request);

            assertThat(response.getName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("Should save two UserLog entries — one ACCESS, one REFRESH")
        void shouldSaveTwoUserLogEntries() {
            LoginRequestDto request = LoginRequestDtoFixture.valid();
            stubHappyPath(request);

            authService.login(request);

            verify(userLogRepository, times(2)).save(any());
        }

        @Test
        @DisplayName("Should call generateAccessToken and generateRefreshToken exactly once each")
        void shouldCallBothTokenGenerators() {
            LoginRequestDto request = LoginRequestDtoFixture.valid();
            stubHappyPath(request);

            authService.login(request);

            verify(jwtService, times(1)).generateAccessToken(activeUser);
            verify(jwtService, times(1)).generateRefreshToken(activeUser);
        }

        @Test
        @DisplayName("UserLog entries must reference the authenticated user")
        void userLogsMustReferenceAuthenticatedUser() {
            LoginRequestDto request = LoginRequestDtoFixture.valid();
            stubHappyPath(request);

            authService.login(request);

            ArgumentCaptor<com.shop.auth.entity.UserLog> captor =
                    ArgumentCaptor.forClass(com.shop.auth.entity.UserLog.class);
            verify(userLogRepository, times(2)).save(captor.capture());
            captor.getAllValues().forEach(ul ->
                    assertThat(ul.getUser()).isSameAs(activeUser));
        }

        @Test
        @DisplayName("UserLog entries must cover both ACCESS and REFRESH token types")
        void userLogsMustCoverBothTokenTypes() {
            LoginRequestDto request = LoginRequestDtoFixture.valid();
            stubHappyPath(request);

            authService.login(request);

            ArgumentCaptor<com.shop.auth.entity.UserLog> captor =
                    ArgumentCaptor.forClass(com.shop.auth.entity.UserLog.class);
            verify(userLogRepository, atLeastOnce()).save(captor.capture());
            assertThat(captor.getAllValues())
                    .extracting(com.shop.auth.entity.UserLog::getTokenType)
                    .containsExactlyInAnyOrder(TokenType.ACCESS, TokenType.REFRESH);
        }
    }

    // ── Invalid credentials ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Invalid credentials")
    class InvalidCredentials {

        @Test
        @DisplayName("Should throw InvalidCredentialsException when user is not found — prevents enumeration")
        void shouldThrowWhenUserNotFound() {
            LoginRequestDto request = LoginRequestDtoFixture.valid();
            when(userRepository.findByEmail(request.getUsername())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessageContaining("Invalid email or password");
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException when password does not match — same type as user-not-found")
        void shouldThrowWhenPasswordDoesNotMatch() {
            LoginRequestDto request = LoginRequestDtoFixture.valid();
            when(userRepository.findByEmail(request.getUsername())).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches(request.getPassword(), activeUser.getPassword())).thenReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("Should never generate tokens when user is not found — but MUST run dummy BCrypt for timing safety")
        void shouldNeverGenerateTokensOnUnknownUser() {
            LoginRequestDto request = LoginRequestDtoFixture.valid();
            when(userRepository.findByEmail(request.getUsername())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(InvalidCredentialsException.class);

            verify(jwtService, never()).generateAccessToken(any());
            verify(jwtService, never()).generateRefreshToken(any());
            // Timing equalizer: passwordEncoder.matches IS called once with the dummy hash
            // to prevent user-enumeration via response-time differences
            verify(passwordEncoder, times(1)).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("Should never generate tokens on bad password")
        void shouldNeverGenerateTokensOnBadPassword() {
            LoginRequestDto request = LoginRequestDtoFixture.valid();
            when(userRepository.findByEmail(request.getUsername())).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches(request.getPassword(), activeUser.getPassword())).thenReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(InvalidCredentialsException.class);

            verify(jwtService, never()).generateAccessToken(any());
        }
    }

    // ── Account status guard ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Account status guard")
    class AccountStatusGuard {

        @ParameterizedTest(name = "status={0}")
        @EnumSource(value = UserStatus.class, names = {"NEW", "INACTIVE", "DELETED"})
        @DisplayName("Should throw UserNotActiveException for non-ACTIVE statuses")
        void shouldThrowForNonActiveStatus(UserStatus status) {
            LoginRequestDto request = LoginRequestDtoFixture.valid();
            activeUser.setStatus(status);
            when(userRepository.findByEmail(request.getUsername())).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches(request.getPassword(), activeUser.getPassword())).thenReturn(true);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UserNotActiveException.class)
                    .hasMessageContaining(status.name());
        }

        @ParameterizedTest(name = "status={0}")
        @EnumSource(value = UserStatus.class, names = {"NEW", "INACTIVE", "DELETED"})
        @DisplayName("Should never issue tokens for non-ACTIVE accounts")
        void shouldNeverIssueTokensForInactiveAccount(UserStatus status) {
            LoginRequestDto request = LoginRequestDtoFixture.valid();
            activeUser.setStatus(status);
            when(userRepository.findByEmail(request.getUsername())).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches(request.getPassword(), activeUser.getPassword())).thenReturn(true);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UserNotActiveException.class);

            verify(jwtService, never()).generateAccessToken(any());
            verify(userLogRepository, never()).save(any());
        }
    }

    // ── Account lockout ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Account lockout")
    class AccountLockout {

        @Test
        @DisplayName("Should throw AccountLockedException when account is currently locked")
        void shouldThrowWhenAccountIsLocked() {
            LoginRequestDto request = LoginRequestDtoFixture.valid();
            activeUser.setLockedUntil(LocalDateTime.now().plusHours(1));
            when(userRepository.findByEmail(request.getUsername())).thenReturn(Optional.of(activeUser));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(AccountLockedException.class)
                    .hasMessageContaining("Account temporarily locked");
        }

        @Test
        @DisplayName("Should never verify password when account is locked — no BCrypt wasted")
        void shouldNeverCheckPasswordWhenLocked() {
            LoginRequestDto request = LoginRequestDtoFixture.valid();
            activeUser.setLockedUntil(LocalDateTime.now().plusMinutes(10));
            when(userRepository.findByEmail(request.getUsername())).thenReturn(Optional.of(activeUser));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(AccountLockedException.class);

            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("Should allow login when lockout period has already expired")
        void shouldAllowLoginAfterLockoutExpires() {
            LoginRequestDto request = LoginRequestDtoFixture.valid();
            activeUser.setLockedUntil(LocalDateTime.now().minusSeconds(1));  // expired
            activeUser.setFailedLoginAttempts(5);
            when(userRepository.findByEmail(request.getUsername())).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches(request.getPassword(), activeUser.getPassword())).thenReturn(true);
            when(jwtService.generateAccessToken(activeUser)).thenReturn("access.token.value");
            when(jwtService.generateRefreshToken(activeUser)).thenReturn("refresh.token.value");
            when(jwtService.extractExpiration(anyString())).thenReturn(new Date(System.currentTimeMillis() + 900_000));
            when(userLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            LoginResponseDto response = authService.login(request);

            assertThat(response.getAccessToken()).isEqualTo("access.token.value");
        }

        @Test
        @DisplayName("Should increment failed attempt counter on bad password")
        void shouldIncrementFailedAttemptsOnBadPassword() {
            LoginRequestDto request = LoginRequestDtoFixture.valid();
            activeUser.setFailedLoginAttempts(1);
            when(userRepository.findByEmail(request.getUsername())).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches(request.getPassword(), activeUser.getPassword())).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(InvalidCredentialsException.class);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getFailedLoginAttempts()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should lock account after reaching MAX_FAILED_ATTEMPTS (5)")
        void shouldLockAccountAfterMaxFailedAttempts() {
            LoginRequestDto request = LoginRequestDtoFixture.valid();
            activeUser.setFailedLoginAttempts(4);  // one more will trigger lockout
            when(userRepository.findByEmail(request.getUsername())).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches(request.getPassword(), activeUser.getPassword())).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(InvalidCredentialsException.class);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User saved = captor.getValue();
            assertThat(saved.getFailedLoginAttempts()).isEqualTo(5);
            assertThat(saved.getLockedUntil())
                    .as("Account must be locked after 5 failures")
                    .isNotNull()
                    .isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("Should reset failed attempt counter on successful login")
        void shouldResetCounterOnSuccess() {
            LoginRequestDto request = LoginRequestDtoFixture.valid();
            activeUser.setFailedLoginAttempts(3);  // prior failures
            when(userRepository.findByEmail(request.getUsername())).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches(request.getPassword(), activeUser.getPassword())).thenReturn(true);
            when(jwtService.generateAccessToken(activeUser)).thenReturn("access.token.value");
            when(jwtService.generateRefreshToken(activeUser)).thenReturn("refresh.token.value");
            when(jwtService.extractExpiration(anyString())).thenReturn(new Date(System.currentTimeMillis() + 900_000));
            when(userLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            authService.login(request);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getFailedLoginAttempts()).isZero();
            assertThat(captor.getValue().getLockedUntil()).isNull();
        }

        @Test
        @DisplayName("Should not call userRepository.save on success when counter is already zero")
        void shouldNotSaveUserWhenCounterAlreadyZero() {
            LoginRequestDto request = LoginRequestDtoFixture.valid();
            // activeUser has failedLoginAttempts=0 and lockedUntil=null by default
            stubHappyPath(request);

            authService.login(request);

            verify(userRepository, never()).save(any(User.class));
        }
    }
}
