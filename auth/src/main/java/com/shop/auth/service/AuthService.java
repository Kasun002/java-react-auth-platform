package com.shop.auth.service;

import com.shop.auth.dto.ChangePasswordRequestDto;
import com.shop.auth.dto.ForgotPasswordRequestDto;
import com.shop.auth.dto.LoginRequestDto;
import com.shop.auth.dto.LoginResponseDto;
import com.shop.auth.dto.RefreshTokenRequestDto;
import com.shop.auth.dto.RefreshTokenResponseDto;
import com.shop.auth.dto.RegisterRequestDto;
import com.shop.auth.dto.ResendOtpRequestDto;
import com.shop.auth.dto.ResetPasswordRequestDto;
import com.shop.auth.dto.VerifyOtpRequestDto;

public interface AuthService {

    void register(RegisterRequestDto request);

    LoginResponseDto login(LoginRequestDto request);

    void verifyOtp(VerifyOtpRequestDto request);

    void resendOtp(ResendOtpRequestDto request);

    /**
     * Changes the authenticated user's password.
     *
     * <p>Steps: verify current password → enforce history → encode and save →
     * record in history → update {@code passwordChangedAt} → invalidate all
     * existing tokens so the user must re-authenticate on all devices.</p>
     *
     * @param accessToken the raw Bearer token (used to identify the user)
     * @param request     contains the current and new passwords
     */
    void changePassword(String accessToken, ChangePasswordRequestDto request);

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
     * Initiates a password reset flow for the given email.
     *
     * <p>If the email is not found the call still returns successfully — this prevents
     * account enumeration. A single-use reset token is stored in Redis with a short TTL
     * and a reset link is sent to the user's email.</p>
     */
    void forgotPassword(ForgotPasswordRequestDto request);

    /**
     * Completes the password reset flow.
     *
     * <p>Validates the reset token against Redis, enforces password history,
     * saves the new password, deletes the token (single-use), and invalidates
     * all existing sessions.</p>
     *
     * @throws com.shop.auth.exception.PasswordResetTokenException if the token is
     *         missing, expired, or already consumed
     */
    void resetPassword(ResetPasswordRequestDto request);

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
