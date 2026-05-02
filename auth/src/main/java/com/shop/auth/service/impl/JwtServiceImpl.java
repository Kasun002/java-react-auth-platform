package com.shop.auth.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

import javax.crypto.SecretKey;

import com.shop.auth.entity.User;
import com.shop.auth.service.JwtService;
import com.shop.auth.utils.TokenType;

import io.jsonwebtoken.Claims;
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
    public boolean isTokenValid(String token, String username) {
        return extractUsername(token).equals(username) && !isTokenExpired(token);
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
            .claim("userId",    user.getId())
            .claim("role",      user.getRole())
            .claim("tokenType", tokenType.name())
            .issuedAt(now)
            .expiration(expiry)
            .signWith(cachedSigningKey)
            .compact();
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
