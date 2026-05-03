package com.shop.auth.service;

import com.shop.auth.dto.LoginRequestDto;
import com.shop.auth.dto.LoginResponseDto;
import com.shop.auth.dto.RefreshTokenRequestDto;
import com.shop.auth.dto.RefreshTokenResponseDto;
import com.shop.auth.dto.RegisterRequestDto;
import com.shop.auth.dto.ResendOtpRequestDto;
import com.shop.auth.dto.VerifyOtpRequestDto;

public interface AuthService {

    void register(RegisterRequestDto request);

    LoginResponseDto login(LoginRequestDto request);

    void verifyOtp(VerifyOtpRequestDto request);

    void resendOtp(ResendOtpRequestDto request);

    /**
     * Issues a new access token and a new refresh token in exchange for a valid,
     * non-revoked refresh token (refresh token rotation).
     *
     * <p>The supplied refresh token is immediately blacklisted after the new pair
     * is generated. If the token has already been used (i.e., it is blacklisted),
     * the call is rejected — this detects refresh token theft.</p>
     *
     * @param request contains the refresh token to exchange
     * @return a new access + refresh token pair
     */
    RefreshTokenResponseDto refresh(RefreshTokenRequestDto request);

    /**
     * Revokes the current session by blacklisting both the access token and the
     * refresh token in Redis.  Each JTI is stored with a TTL equal to the token's
     * remaining lifetime so the Redis entry self-expires and never grows unbounded.
     *
     * @param accessToken  the raw Bearer token from the Authorization header
     * @param refreshToken the raw refresh token supplied by the client (may be null)
     */
    void logout(String accessToken, String refreshToken);
}
