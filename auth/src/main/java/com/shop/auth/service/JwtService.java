package com.shop.auth.service;

import java.util.Date;
import java.util.List;

import com.shop.auth.entity.User;

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
}
