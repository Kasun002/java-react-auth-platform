package com.org.auth.service.impl;

import java.util.List;

import com.org.auth.dto.RegisterRequestDto;
import com.org.auth.entity.Address;
import com.org.auth.entity.User;
import com.org.auth.exception.EmailAlreadyExistsException;
import com.org.auth.fixtures.AddressDtoFixture;
import com.org.auth.fixtures.RegisterRequestDtoFixture;
import com.org.auth.repository.UserGroupRepository;
import com.org.auth.repository.UserRepository;
import com.org.auth.service.OtpService;
import com.org.auth.service.PasswordPolicyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AuthServiceImpl")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class AuthServiceImplTest {

    @Mock private UserRepository        userRepository;
    @Mock private UserGroupRepository   userGroupRepository;
    @Mock private PasswordEncoder       passwordEncoder;
    @Mock private OtpService            otpService;           // void mock — silently does nothing, no stub needed
    @Mock private PasswordPolicyService passwordPolicyService; // void mock — recordPasswordChange is a no-op in tests
    @InjectMocks private AuthServiceImpl authService;

    // ── Helper ──────────────────────────────────────────────────────────────

    /** Common stubbing for the happy-path flow. */
    private void stubHappyPath(RegisterRequestDto request) {
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private User capturePersistedUser() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        return captor.getValue();
    }

    // ── Registration success ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Successful registration")
    class Success {

        @Test
        @DisplayName("Should complete without exception when all fields are valid")
        void shouldCompleteWithoutException() {
            RegisterRequestDto request = RegisterRequestDtoFixture.valid();
            stubHappyPath(request);
            assertThatNoException().isThrownBy(() -> authService.register(request));
        }

        @Test
        @DisplayName("Should persist user with correct name and email")
        void shouldPersistCorrectNameAndEmail() {
            RegisterRequestDto request = RegisterRequestDtoFixture.valid();
            stubHappyPath(request);

            authService.register(request);

            User saved = capturePersistedUser();
            assertThat(saved.getName()).isEqualTo("John Doe");
            assertThat(saved.getEmail()).isEqualTo("john.doe@example.com");
        }

        @Test
        @DisplayName("Should call repository save exactly once")
        void shouldCallSaveExactlyOnce() {
            RegisterRequestDto request = RegisterRequestDtoFixture.valid();
            stubHappyPath(request);

            authService.register(request);

            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("Should check email uniqueness before attempting to persist — enforces fail-fast")
        void shouldCheckEmailBeforePersisting() {
            RegisterRequestDto request = RegisterRequestDtoFixture.valid();
            stubHappyPath(request);

            authService.register(request);

            InOrder inOrder = inOrder(userRepository);
            inOrder.verify(userRepository).existsByEmail(request.getEmail());
            inOrder.verify(userRepository).save(any(User.class));
        }
    }

    // ── Password security ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Password security")
    class PasswordSecurity {

        @Test
        @DisplayName("Should never persist plain-text password — CRITICAL security control")
        void shouldNeverStorePlainTextPassword() {
            String plainText = "Secret@123";
            RegisterRequestDto request = RegisterRequestDtoFixture.withPassword(plainText);
            when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
            when(passwordEncoder.encode(plainText)).thenReturn("$2a$10$BCryptHashedValue");
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authService.register(request);

            User saved = capturePersistedUser();
            assertThat(saved.getPassword())
                .as("Password must be hashed, never plain-text")
                .isNotEqualTo(plainText)
                .startsWith("$2a$");
        }

        @Test
        @DisplayName("Should call password encoder exactly once per registration")
        void shouldEncodePasswordExactlyOnce() {
            RegisterRequestDto request = RegisterRequestDtoFixture.valid();
            stubHappyPath(request);

            authService.register(request);

            verify(passwordEncoder, times(1)).encode(request.getPassword());
        }
    }

    // ── Address linking ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Address linking")
    class AddressLinking {

        @Test
        @DisplayName("Should persist a single address linked to the user")
        void shouldPersistSingleAddress() {
            RegisterRequestDto request = RegisterRequestDtoFixture.valid();
            stubHappyPath(request);

            authService.register(request);

            User saved = capturePersistedUser();
            assertThat(saved.getAddresses()).hasSize(1);
            assertThat(saved.getAddresses().get(0).getUser()).isSameAs(saved);
        }

        @Test
        @DisplayName("Should persist multiple addresses — all linked back to same user")
        void shouldPersistMultipleAddresses() {
            RegisterRequestDto request = RegisterRequestDtoFixture.withAddresses(
                List.of(AddressDtoFixture.valid(), AddressDtoFixture.valid(), AddressDtoFixture.valid())
            );
            stubHappyPath(request);

            authService.register(request);

            User saved = capturePersistedUser();
            assertThat(saved.getAddresses()).hasSize(3);
            saved.getAddresses().forEach(address ->
                assertThat(address.getUser())
                    .as("Every address must reference the parent user")
                    .isSameAs(saved));
        }

        @Test
        @DisplayName("Should map address fields correctly from DTO to entity")
        void shouldMapAddressFieldsCorrectly() {
            RegisterRequestDto request = RegisterRequestDtoFixture.valid();
            stubHappyPath(request);

            authService.register(request);

            Address address = capturePersistedUser().getAddresses().get(0);
            assertThat(address.getAddressLine1()).isEqualTo("123 Main Street");
            assertThat(address.getCountry()).isEqualTo("Sri Lanka");
        }
    }

    // ── Duplicate email ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Duplicate email guard")
    class DuplicateEmail {

        @Test
        @DisplayName("Should throw EmailAlreadyExistsException when email is already registered")
        void shouldThrowWhenEmailExists() {
            RegisterRequestDto request = RegisterRequestDtoFixture.valid();
            when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining(request.getEmail());
        }

        @Test
        @DisplayName("Should never call save when email already exists")
        void shouldNeverPersistWhenEmailExists() {
            RegisterRequestDto request = RegisterRequestDtoFixture.valid();
            when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);

            verify(userRepository, never()).save(any());
            verify(passwordEncoder, never()).encode(anyString());
        }
    }
}
