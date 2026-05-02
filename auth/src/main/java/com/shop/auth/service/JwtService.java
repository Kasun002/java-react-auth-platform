package com.shop.auth.service;

import java.util.Date;

import com.shop.auth.entity.User;

public interface JwtService {

    String generateAccessToken(User user);

    String generateRefreshToken(User user);

    String extractUsername(String token);

    Date extractExpiration(String token);

    boolean isTokenValid(String token, String username);
}
