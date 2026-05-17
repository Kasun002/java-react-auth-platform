package com.shop.auth.service;

import com.shop.auth.entity.User;

/**
 * Enforces banking-grade password policy rules that go beyond simple format validation:
 * <ul>
 *   <li><b>History</b> — rejects passwords that match any of the user's last N hashes.</li>
 *   <li><b>Record</b> — persists the new password hash to history after a successful change.</li>
 * </ul>
 *
 * <p>Password complexity (length, character classes) is handled declaratively via
 * {@link com.shop.auth.validation.StrongPassword} on the DTO, not here.</p>
 *
 * <p>Password age enforcement (max N days) is handled in
 * {@link impl.AuthServiceImpl} during login, not here, because it depends on
 * the user entity rather than a candidate plain-text password.</p>
 */
public interface PasswordPolicyService {

    /**
     * Checks whether {@code plainPassword} matches any of the user's recent
     * password history.  Throws {@link com.shop.auth.exception.PasswordHistoryViolationException}
     * if a match is found.
     *
     * @param user          the user whose history to check
     * @param plainPassword the candidate plain-text password (not yet encoded)
     */
    void enforceHistory(User user, String plainPassword);

    /**
     * Persists the encoded password hash to the user's password history and
     * prunes entries beyond the configured history window.
     *
     * <p>Must be called <em>after</em> the new password has been saved to the
     * user record so the history stays consistent.</p>
     *
     * @param user            the user whose history to update
     * @param encodedPassword the BCrypt-encoded password that was just set
     */
    void recordPasswordChange(User user, String encodedPassword);
}
