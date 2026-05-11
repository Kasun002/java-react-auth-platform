package com.shop.auth.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import com.shop.auth.entity.Role;
import com.shop.auth.entity.User;
import com.shop.auth.entity.UserGroup;
import com.shop.auth.service.JwtService;
import com.shop.auth.utils.TokenType;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtServiceImpl implements JwtService {

    private static final String ISSUER   = "auth-service";
    private static final String AUDIENCE = "shop-platform";

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.access-token-expiry-ms}")
    private long accessTokenExpiryMs;

    @Value("${app.jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;

    private SecretKey cachedSigningKey;

    @PostConstruct
    void init() {
        cachedSigningKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generateAccessToken(User user) {
        return buildToken(user, accessTokenExpiryMs, TokenType.ACCESS);
    }

    @Override
    public String generateRefreshToken(User user) {
        return buildToken(user, refreshTokenExpiryMs, TokenType.REFRESH);
    }

    @Override
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    @Override
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    @Override
    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public boolean isTokenValid(String token, String username) {
        try {
            return extractUsername(token).equals(username) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("tokenType", String.class));
    }

    @Override
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> {
            Object val = claims.get("userId");
            if (val instanceof Long)    return (Long) val;
            if (val instanceof Integer) return ((Integer) val).longValue();
            return null;
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> extractPermissions(String token) {
        return extractClaim(token, claims -> {
            Object val = claims.get("permissions");
            return val instanceof List ? (List<String>) val : Collections.emptyList();
        });
    }

    @Override
    public String extractJti(String token) {
        return extractClaim(token, Claims::getId);
    }

    @Override
    public Date extractIssuedAt(String token) {
        return extractClaim(token, Claims::getIssuedAt);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> extractGroups(String token) {
        return extractClaim(token, claims -> {
            Object val = claims.get("groups");
            return val instanceof List ? (List<String>) val : Collections.emptyList();
        });
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private String buildToken(User user, long expiryMs, TokenType tokenType) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expiryMs);
        return Jwts.builder()
            .id(UUID.randomUUID().toString())       // jti — enables per-token revocation
            .issuer(ISSUER)                         // iss — scopes token to this service
            .audience().add(AUDIENCE).and()         // aud — prevents cross-service replay
            .subject(user.getEmail())
            .claim("userId",      user.getId())
            .claim("tokenType",   tokenType.name())
            .claim("permissions", computePermissions(user))
            .claim("groups",      computeGroupNames(user))
            .issuedAt(now)
            .expiration(expiry)
            .signWith(cachedSigningKey)
            .compact();
    }

    /**
     * Computes the effective permission codes for a user by unioning:
     * - permissions from all group-assigned roles
     * - permissions from directly assigned roles
     *
     * <p>Called within the login transaction so LAZY collections are accessible.</p>
     */
    private List<String> computePermissions(User user) {
        Set<String> perms = new HashSet<>();
        for (UserGroup group : user.getGroups()) {
            for (Role role : group.getRoles()) {
                role.getPermissions().forEach(p -> perms.add(p.getCode()));
            }
        }
        for (Role role : user.getDirectRoles()) {
            role.getPermissions().forEach(p -> perms.add(p.getCode()));
        }
        return new ArrayList<>(perms);
    }

    private List<String> computeGroupNames(User user) {
        return user.getGroups().stream()
                .map(UserGroup::getName)
                .collect(Collectors.toList());
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
            .verifyWith(cachedSigningKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return resolver.apply(claims);
    }
}
