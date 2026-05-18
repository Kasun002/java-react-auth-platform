package com.org.auth.service.impl;

import java.util.List;

import com.org.auth.entity.PasswordHistory;
import com.org.auth.entity.User;
import com.org.auth.exception.PasswordHistoryViolationException;
import com.org.auth.repository.PasswordHistoryRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("PasswordPolicyServiceImpl")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class PasswordPolicyServiceImplTest {

    @Mock private PasswordHistoryRepository passwordHistoryRepository;
    @Mock private PasswordEncoder           passwordEncoder;

    @InjectMocks private PasswordPolicyServiceImpl passwordPolicyService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("john.doe@example.com");
    }

    // ── enforceHistory ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("enforceHistory")
    class EnforceHistory {

        @BeforeEach
        void setHistoryCount() {
            ReflectionTestUtils.setField(passwordPolicyService, "historyCount", 12);
        }

        @Test
        @DisplayName("Should pass without exception when the user has no password history")
        void shouldPassWithNoHistory() {
            when(passwordHistoryRepository.findRecentByUser(eq(user), any())).thenReturn(List.of());

            assertThatNoException().isThrownBy(
                    () -> passwordPolicyService.enforceHistory(user, "NewPass@123"));
        }

        @Test
        @DisplayName("Should pass when new password does not match any recent hash")
        void shouldPassWhenNoMatch() {
            PasswordHistory entry = new PasswordHistory();
            entry.setPasswordHash("$2a$10$differentHash");
            when(passwordHistoryRepository.findRecentByUser(eq(user), any()))
                    .thenReturn(List.of(entry));
            when(passwordEncoder.matches("NewPass@123", "$2a$10$differentHash")).thenReturn(false);

            assertThatNoException().isThrownBy(
                    () -> passwordPolicyService.enforceHistory(user, "NewPass@123"));
        }

        @Test
        @DisplayName("Should throw PasswordHistoryViolationException when password matches a recent entry")
        void shouldThrowWhenMatchesHistory() {
            PasswordHistory entry = new PasswordHistory();
            entry.setPasswordHash("$2a$10$recentHash");
            when(passwordHistoryRepository.findRecentByUser(eq(user), any()))
                    .thenReturn(List.of(entry));
            when(passwordEncoder.matches("OldPass@123", "$2a$10$recentHash")).thenReturn(true);

            assertThatThrownBy(() -> passwordPolicyService.enforceHistory(user, "OldPass@123"))
                    .isInstanceOf(PasswordHistoryViolationException.class)
                    .hasMessageContaining("last 12");
        }

        @Test
        @DisplayName("Should check all entries in order and fail on the matching one")
        void shouldCheckAllEntriesAndFailOnMatch() {
            PasswordHistory first  = new PasswordHistory();
            first.setPasswordHash("$2a$10$hash1");
            PasswordHistory second = new PasswordHistory();
            second.setPasswordHash("$2a$10$hash2");

            when(passwordHistoryRepository.findRecentByUser(eq(user), any()))
                    .thenReturn(List.of(first, second));
            when(passwordEncoder.matches("Target@123", "$2a$10$hash1")).thenReturn(false);
            when(passwordEncoder.matches("Target@123", "$2a$10$hash2")).thenReturn(true);

            assertThatThrownBy(() -> passwordPolicyService.enforceHistory(user, "Target@123"))
                    .isInstanceOf(PasswordHistoryViolationException.class);
        }
    }

    // ── recordPasswordChange ──────────────────────────────────────────────────

    @Nested
    @DisplayName("recordPasswordChange")
    class RecordPasswordChange {

        @BeforeEach
        void setHistoryCount() {
            // Use 3 so pruning tests are easy to reason about
            ReflectionTestUtils.setField(passwordPolicyService, "historyCount", 3);
        }

        @Test
        @DisplayName("Should save a new PasswordHistory entry with the correct hash and user reference")
        void shouldSaveNewEntry() {
            when(passwordHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(passwordHistoryRepository.findAllIdsByUserOrderByCreatedAtDesc(user))
                    .thenReturn(List.of(1L)); // 1 entry — within limit of 3

            passwordPolicyService.recordPasswordChange(user, "$2a$10$newHash");

            ArgumentCaptor<PasswordHistory> captor = ArgumentCaptor.forClass(PasswordHistory.class);
            verify(passwordHistoryRepository).save(captor.capture());
            assertThat(captor.getValue().getPasswordHash()).isEqualTo("$2a$10$newHash");
            assertThat(captor.getValue().getUser()).isSameAs(user);
        }

        @Test
        @DisplayName("Should not prune when entry count is within the history limit")
        void shouldNotPruneWhenWithinLimit() {
            when(passwordHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(passwordHistoryRepository.findAllIdsByUserOrderByCreatedAtDesc(user))
                    .thenReturn(List.of(3L, 2L, 1L)); // exactly historyCount=3

            passwordPolicyService.recordPasswordChange(user, "$2a$10$hash");

            verify(passwordHistoryRepository, never()).deleteByIdIn(any());
        }

        @Test
        @DisplayName("Should prune the oldest entry when history exceeds the configured limit")
        void shouldPruneWhenOverLimit() {
            when(passwordHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            // 4 IDs (newest first) — historyCount=3, so ID 1 (oldest) must be deleted
            when(passwordHistoryRepository.findAllIdsByUserOrderByCreatedAtDesc(user))
                    .thenReturn(List.of(4L, 3L, 2L, 1L));

            passwordPolicyService.recordPasswordChange(user, "$2a$10$hash");

            ArgumentCaptor<List<Long>> deleteCaptor = ArgumentCaptor.forClass(List.class);
            verify(passwordHistoryRepository).deleteByIdIn(deleteCaptor.capture());
            assertThat(deleteCaptor.getValue()).containsExactly(1L);
        }

        @Test
        @DisplayName("Should prune multiple old entries when history is significantly over limit")
        void shouldPruneMultipleOldEntries() {
            when(passwordHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            // 6 IDs (newest first) — historyCount=3, IDs 3,2,1 must be deleted
            when(passwordHistoryRepository.findAllIdsByUserOrderByCreatedAtDesc(user))
                    .thenReturn(List.of(6L, 5L, 4L, 3L, 2L, 1L));

            passwordPolicyService.recordPasswordChange(user, "$2a$10$hash");

            ArgumentCaptor<List<Long>> deleteCaptor = ArgumentCaptor.forClass(List.class);
            verify(passwordHistoryRepository).deleteByIdIn(deleteCaptor.capture());
            assertThat(deleteCaptor.getValue()).containsExactlyInAnyOrder(3L, 2L, 1L);
        }
    }
}
