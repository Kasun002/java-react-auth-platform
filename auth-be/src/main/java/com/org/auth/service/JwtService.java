package com.org.auth.service;

import java.util.Date;
import java.util.List;

import com.org.auth.entity.User;

public interface JwtService {

    String generateAccessToken(User user);

    String generateRefreshToken(User user);

    String extractUsername(String token);

    Date extractExpiration(String token);

    /** Validates signature and expiry without requiring a known username. Used by the JWT filter. */
    boolean isTokenValid(String token);

    /** Validates signature, expiry, and that the subject matches the given username. */
    boolean isTokenValid(String token, String username);

    /** Extracts the {@code tokenType} claim (ACCESS | REFRESH). */
    String extractTokenType(String token);

    /** Extracts the {@code userId} claim. */
    Long extractUserId(String token);

    /** Extracts the {@code permissions} claim as a list of permission codes. */
    List<String> extractPermissions(String token);

    /** Extracts the {@code groups} claim as a list of group names. */
    List<String> extractGroups(String token);

    /** Extracts the {@code jti} (JWT ID) claim — used for per-token revocation. */
    String extractJti(String token);

    /** Extracts the {@code iat} (issued-at) claim — used for user-level session invalidation. */
    Date extractIssuedAt(String token);

    /** Extracts the {@code name} claim — used to populate the audit log actor name without a DB lookup. */
    String extractName(String token);
}
