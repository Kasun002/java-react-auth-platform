package com.shop.auth.service;

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
}
