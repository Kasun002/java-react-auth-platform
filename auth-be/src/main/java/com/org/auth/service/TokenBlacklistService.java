package com.org.auth.service;

public interface TokenBlacklistService {

    /**
     * Adds the given JWT ID to the blacklist with the specified TTL.
     * The entry expires automatically when the original token would have expired.
     *
     * @param jti        the JWT ID claim from the token
     * @param ttlSeconds remaining lifetime of the token in seconds
     */
    void blacklist(String jti, long ttlSeconds);

    /**
     * Returns {@code true} if the given JWT ID has been blacklisted (revoked).
     *
     * @param jti the JWT ID claim to check
     */
    boolean isBlacklisted(String jti);

    /**
     * Records a user-level session invalidation event. Any token issued before
     * this moment for the given user will be rejected by the filter.
     *
     * <p>Used on password change, account suspension, and admin-forced logout.
     * The entry expires automatically after {@code ttlSeconds} so Redis never
     * holds stale entries beyond the maximum token lifetime.</p>
     *
     * @param userId     the user whose tokens should be invalidated
     * @param ttlSeconds TTL matching the longest-lived token type (refresh token expiry)
     */
    void invalidateAllUserTokens(Long userId, long ttlSeconds);

    /**
     * Returns {@code true} if the token was issued before the user's last
     * global invalidation event (password change, suspension, etc.).
     *
     * @param userId        the user who owns the token
     * @param tokenIssuedAt the {@code iat} claim from the token as an {@link java.time.Instant}
     */
    boolean isUserTokensInvalidated(Long userId, java.time.Instant tokenIssuedAt);
}
