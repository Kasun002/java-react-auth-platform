package com.org.auth.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.org.auth.entity.PasswordHistory;
import com.org.auth.entity.User;
import com.org.auth.exception.PasswordHistoryViolationException;
import com.org.auth.repository.PasswordHistoryRepository;
import com.org.auth.service.PasswordPolicyService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordPolicyServiceImpl implements PasswordPolicyService {

    @Value("${app.security.password.history-count:12}")
    private int historyCount;

    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    // ── History enforcement ───────────────────────────────────────────────────

    @Override
    public void enforceHistory(User user, String plainPassword) {
        List<PasswordHistory> recent = passwordHistoryRepository
                .findRecentByUser(user, PageRequest.of(0, historyCount));

        for (PasswordHistory entry : recent) {
            if (passwordEncoder.matches(plainPassword, entry.getPasswordHash())) {
                log.warn("Password history violation for userId=[{}]", user.getId());
                throw new PasswordHistoryViolationException(historyCount);
            }
        }
    }

    // ── History recording ─────────────────────────────────────────────────────

    /**
     * Saves the encoded password to history, then prunes entries older than the
     * configured history window so the table never grows unbounded.
     */
    @Override
    @Transactional
    public void recordPasswordChange(User user, String encodedPassword) {
        PasswordHistory entry = new PasswordHistory();
        entry.setUser(user);
        entry.setPasswordHash(encodedPassword);
        passwordHistoryRepository.save(entry);

        pruneOldEntries(user);
        log.debug("Password history recorded for userId=[{}]", user.getId());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Deletes history entries beyond the configured {@code historyCount} window,
     * keeping only the most recent N entries for the user.
     */
    private void pruneOldEntries(User user) {
        List<Long> allIds = passwordHistoryRepository.findAllIdsByUserOrderByCreatedAtDesc(user);
        if (allIds.size() > historyCount) {
            List<Long> idsToDelete = allIds.subList(historyCount, allIds.size());
            passwordHistoryRepository.deleteByIdIn(idsToDelete);
            log.debug("Pruned [{}] old password history entries for userId=[{}]",
                    idsToDelete.size(), user.getId());
        }
    }
}
