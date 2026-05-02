package com.shop.auth.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;

import com.shop.auth.entity.OtpVerification;
import com.shop.auth.entity.User;
import com.shop.auth.exception.OtpExpiredException;
import com.shop.auth.exception.OtpInvalidException;
import com.shop.auth.exception.OtpMaxAttemptsException;
import com.shop.auth.exception.OtpResendLimitException;
import com.shop.auth.repository.OtpVerificationRepository;
import com.shop.auth.repository.UserRepository;
import com.shop.auth.service.EmailService;
import com.shop.auth.utils.Role;
import com.shop.auth.utils.UserStatus;
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
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("OtpServiceImpl")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class OtpServiceImplTest {

    @Mock private OtpVerificationRepository otpVerificationRepository;
    @Mock private UserRepository            userRepository;
    @Mock private EmailService              emailService;
    @InjectMocks private OtpServiceImpl otpService;

    private User newUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(otpService, "otpExpiryMinutes",  10);
        ReflectionTestUtils.setField(otpService, "maxAttempts",        3);
        ReflectionTestUtils.setField(otpService, "maxResendsPerHour",  3);

        newUser = new User();
        newUser.setId(1L);
        newUser.setName("John Doe");
        newUser.setEmail("john.doe@example.com");
        newUser.setStatus(UserStatus.NEW);
        newUser.setRole(Role.USER);
    }

    // ── generateAndSend ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("generateAndSend")
    class GenerateAndSend {

        @Test
        @DisplayName("Should invalidate all existing unused OTPs before saving the new one")
        void shouldInvalidateOldOtpsFirst() {
            when(otpVerificationRepository.save(any(OtpVerification.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(otpVerificationRepository).invalidateAllUnusedForUser(newUser);
            doNothing().when(emailService).sendOtp(anyString(), anyString(), anyString(), anyInt());

            otpService.generateAndSend(newUser);

            verify(otpVerificationRepository).invalidateAllUnusedForUser(newUser);
        }

        @Test
        @DisplayName("Should save an OTP record to the repository")
        void shouldSaveOtpRecord() {
            when(otpVerificationRepository.save(any(OtpVerification.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(otpVerificationRepository).invalidateAllUnusedForUser(newUser);
            doNothing().when(emailService).sendOtp(anyString(), anyString(), anyString(), anyInt());

            otpService.generateAndSend(newUser);

            verify(otpVerificationRepository, times(1)).save(any(OtpVerification.class));
        }

        @Test
        @DisplayName("Should never store the raw OTP — only its SHA-256 hash")
        void shouldNeverStoreRawOtp() {
            when(otpVerificationRepository.save(any(OtpVerification.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(otpVerificationRepository).invalidateAllUnusedForUser(newUser);

            ArgumentCaptor<String> rawOtpCaptor = ArgumentCaptor.forClass(String.class);
            doNothing().when(emailService).sendOtp(anyString(), anyString(), rawOtpCaptor.capture(), anyInt());

            otpService.generateAndSend(newUser);

            ArgumentCaptor<OtpVerification> savedCaptor = ArgumentCaptor.forClass(OtpVerification.class);
            verify(otpVerificationRepository).save(savedCaptor.capture());

            String rawOtp    = rawOtpCaptor.getValue();
            String savedHash = savedCaptor.getValue().getOtpHash();

            assertThat(savedHash).isNotEqualTo(rawOtp);
            assertThat(savedHash).hasSize(64);     // SHA-256 hex = 64 chars
        }

        @Test
        @DisplayName("Should set expiry to approximately now + configured minutes")
        void shouldSetCorrectExpiry() {
            when(otpVerificationRepository.save(any(OtpVerification.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(otpVerificationRepository).invalidateAllUnusedForUser(newUser);
            doNothing().when(emailService).sendOtp(anyString(), anyString(), anyString(), anyInt());

            otpService.generateAndSend(newUser);

            ArgumentCaptor<OtpVerification> captor = ArgumentCaptor.forClass(OtpVerification.class);
            verify(otpVerificationRepository).save(captor.capture());

            assertThat(captor.getValue().getExpiresAt())
                    .isAfter(LocalDateTime.now())
                    .isBefore(LocalDateTime.now().plusMinutes(11));
        }

        @Test
        @DisplayName("Should send email with the raw OTP to the user's address")
        void shouldSendEmailToUser() {
            when(otpVerificationRepository.save(any(OtpVerification.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(otpVerificationRepository).invalidateAllUnusedForUser(newUser);
            doNothing().when(emailService).sendOtp(eq("john.doe@example.com"), eq("John Doe"), anyString(), eq(10));

            otpService.generateAndSend(newUser);

            verify(emailService).sendOtp(eq("john.doe@example.com"), eq("John Doe"), anyString(), eq(10));
        }

        @Test
        @DisplayName("Generated OTP must be exactly 6 digits and include full range (0-padded)")
        void generatedOtpMustBeSixDigits() {
            when(otpVerificationRepository.save(any(OtpVerification.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(otpVerificationRepository).invalidateAllUnusedForUser(newUser);

            ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
            doNothing().when(emailService).sendOtp(anyString(), anyString(), otpCaptor.capture(), anyInt());

            otpService.generateAndSend(newUser);

            assertThat(otpCaptor.getValue())
                    .matches("\\d{6}")
                    .hasSize(6);   // always exactly 6 digits (zero-padded)
        }
    }

    // ── verify ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("verify")
    class Verify {

        private OtpVerification buildRecord(String rawOtp, boolean expired, int attempts) {
            OtpVerification r = new OtpVerification();
            r.setUser(newUser);
            r.setOtpHash(sha256HexForTest(rawOtp));
            r.setExpiresAt(expired
                    ? LocalDateTime.now().minusMinutes(1)
                    : LocalDateTime.now().plusMinutes(10));
            r.setAttempts(attempts);
            r.setUsed(false);
            return r;
        }

        @Test
        @DisplayName("Should activate user and mark OTP as used on correct OTP")
        void shouldActivateUserOnCorrectOtp() {
            String rawOtp = "482910";
            OtpVerification record = buildRecord(rawOtp, false, 0);

            when(userRepository.findByEmail(newUser.getEmail())).thenReturn(Optional.of(newUser));
            when(otpVerificationRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(newUser))
                    .thenReturn(Optional.of(record));
            when(otpVerificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            otpService.verify(newUser.getEmail(), rawOtp);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(record.isUsed()).isTrue();
        }

        @Test
        @DisplayName("Should throw OtpInvalidException when OTP does not match")
        void shouldThrowOnMismatch() {
            OtpVerification record = buildRecord("482910", false, 0);
            when(userRepository.findByEmail(newUser.getEmail())).thenReturn(Optional.of(newUser));
            when(otpVerificationRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(newUser))
                    .thenReturn(Optional.of(record));
            when(otpVerificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThatThrownBy(() -> otpService.verify(newUser.getEmail(), "000000"))
                    .isInstanceOf(OtpInvalidException.class);
        }

        @Test
        @DisplayName("Should increment attempt counter on wrong OTP")
        void shouldIncrementAttemptsOnMismatch() {
            OtpVerification record = buildRecord("482910", false, 1);
            when(userRepository.findByEmail(newUser.getEmail())).thenReturn(Optional.of(newUser));
            when(otpVerificationRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(newUser))
                    .thenReturn(Optional.of(record));
            when(otpVerificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThatThrownBy(() -> otpService.verify(newUser.getEmail(), "000000"))
                    .isInstanceOf(OtpInvalidException.class);

            assertThat(record.getAttempts()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should throw OtpExpiredException when OTP record is past expiry")
        void shouldThrowOnExpiredOtp() {
            OtpVerification record = buildRecord("482910", true, 0);
            when(userRepository.findByEmail(newUser.getEmail())).thenReturn(Optional.of(newUser));
            when(otpVerificationRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(newUser))
                    .thenReturn(Optional.of(record));

            assertThatThrownBy(() -> otpService.verify(newUser.getEmail(), "482910"))
                    .isInstanceOf(OtpExpiredException.class);
        }

        @Test
        @DisplayName("Should throw OtpExpiredException when no active OTP record exists")
        void shouldThrowWhenNoRecord() {
            when(userRepository.findByEmail(newUser.getEmail())).thenReturn(Optional.of(newUser));
            when(otpVerificationRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(newUser))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> otpService.verify(newUser.getEmail(), "482910"))
                    .isInstanceOf(OtpExpiredException.class);
        }

        @Test
        @DisplayName("Should throw OtpMaxAttemptsException when attempts >= maxAttempts")
        void shouldThrowOnMaxAttempts() {
            OtpVerification record = buildRecord("482910", false, 3); // already at limit
            when(userRepository.findByEmail(newUser.getEmail())).thenReturn(Optional.of(newUser));
            when(otpVerificationRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(newUser))
                    .thenReturn(Optional.of(record));

            assertThatThrownBy(() -> otpService.verify(newUser.getEmail(), "482910"))
                    .isInstanceOf(OtpMaxAttemptsException.class);

            verify(otpVerificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should return silently when account is already ACTIVE — idempotent")
        void shouldBeIdempotentForActiveUser() {
            newUser.setStatus(UserStatus.ACTIVE);
            when(userRepository.findByEmail(newUser.getEmail())).thenReturn(Optional.of(newUser));

            otpService.verify(newUser.getEmail(), "482910");  // no exception

            verify(otpVerificationRepository, never()).findTopByUserAndUsedFalseOrderByCreatedAtDesc(any());
        }

        @Test
        @DisplayName("Should throw OtpInvalidException when email is unknown — prevents enumeration")
        void shouldThrowOnUnknownEmail() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> otpService.verify("unknown@example.com", "482910"))
                    .isInstanceOf(OtpInvalidException.class);
        }
    }

    // ── resend ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("resend")
    class Resend {

        @Test
        @DisplayName("Should throw OtpResendLimitException when resend rate limit is exceeded")
        void shouldThrowWhenResendLimitExceeded() {
            when(userRepository.findByEmail(newUser.getEmail())).thenReturn(Optional.of(newUser));
            when(otpVerificationRepository.countByUserAndCreatedAtAfter(eq(newUser), any()))
                    .thenReturn(3L);  // already at limit

            assertThatThrownBy(() -> otpService.resend(newUser.getEmail()))
                    .isInstanceOf(OtpResendLimitException.class);
        }

        @Test
        @DisplayName("Should invalidate old OTPs and issue a new one on valid resend")
        void shouldInvalidateOldOtpsOnResend() {
            when(userRepository.findByEmail(newUser.getEmail())).thenReturn(Optional.of(newUser));
            when(otpVerificationRepository.countByUserAndCreatedAtAfter(eq(newUser), any()))
                    .thenReturn(0L);
            doNothing().when(otpVerificationRepository).invalidateAllUnusedForUser(newUser);
            when(otpVerificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(emailService).sendOtp(anyString(), anyString(), anyString(), anyInt());

            otpService.resend(newUser.getEmail());

            verify(otpVerificationRepository).invalidateAllUnusedForUser(newUser);
            verify(otpVerificationRepository).save(any(OtpVerification.class));
        }

        @Test
        @DisplayName("Should throw OtpInvalidException for unknown email — prevents enumeration")
        void shouldThrowForUnknownEmail() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> otpService.resend("unknown@example.com"))
                    .isInstanceOf(OtpInvalidException.class);
        }

        @Test
        @DisplayName("Should return silently when account is already ACTIVE — idempotent")
        void shouldBeIdempotentForActiveUser() {
            newUser.setStatus(UserStatus.ACTIVE);
            when(userRepository.findByEmail(newUser.getEmail())).thenReturn(Optional.of(newUser));

            otpService.resend(newUser.getEmail());

            verify(otpVerificationRepository, never()).countByUserAndCreatedAtAfter(any(), any());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Mirrors OtpServiceImpl.sha256Hex for test fixture preparation. */
    private static String sha256HexForTest(String input) {
        try {
            byte[] hash = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
