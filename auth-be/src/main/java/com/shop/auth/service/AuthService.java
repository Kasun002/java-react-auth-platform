package com.shop.auth.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.shop.auth.dto.AdminCreateUserRequestDto;
import com.shop.auth.dto.AdminUpdateUserRequestDto;
import com.shop.auth.dto.ChangePasswordRequestDto;
import com.shop.auth.dto.ForgotPasswordRequestDto;
import com.shop.auth.dto.LoginRequestDto;
import com.shop.auth.dto.LoginResponseDto;
import com.shop.auth.dto.RefreshTokenRequestDto;
import com.shop.auth.dto.RefreshTokenResponseDto;
import com.shop.auth.dto.RegisterRequestDto;
import com.shop.auth.dto.ResendOtpRequestDto;
import com.shop.auth.dto.ResetPasswordRequestDto;
import com.shop.auth.dto.UpdateUserStatusRequestDto;
import com.shop.auth.dto.UserDto;
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

    /**
     * Returns a paginated list of all users with their RBAC context.
     * Passwords and internal security fields are never included in the response.
     */
    Page<UserDto> getUsers(Pageable pageable);

    /**
     * Returns a single user by ID with their full RBAC context.
     *
     * @throws com.shop.auth.exception.ResourceNotFoundException if no user exists with the given ID
     */
    UserDto getUserById(Long userId);

    // ── Admin user management ─────────────────────────────────────────────────

    /**
     * Admin-provisions a new user account, optionally assigning groups and direct roles
     * on the spot. The account is created in ACTIVE state — no OTP verification required.
     * The temporary password is encoded and seeded into the password history.
     *
     * @throws com.shop.auth.exception.EmailAlreadyExistsException if the email is already taken
     */
    UserDto adminCreateUser(AdminCreateUserRequestDto request);

    /**
     * Updates mutable profile fields (name, email, phone) for an existing user.
     * Email changes are allowed but uniqueness is enforced.
     * Existing sessions are NOT invalidated on a profile-only edit.
     *
     * @throws com.shop.auth.exception.ResourceNotFoundException if the user does not exist
     * @throws com.shop.auth.exception.EmailAlreadyExistsException if the new email is already taken
     */
    UserDto adminUpdateUser(Long userId, AdminUpdateUserRequestDto request);

    /**
     * Changes a user's account status.
     * SUSPENDED immediately invalidates all active tokens for that user.
     * Admins cannot change their own status.
     *
     * @param requestingAdminId  the ID of the admin making the request (self-action guard)
     * @throws com.shop.auth.exception.ResourceNotFoundException if the user does not exist
     * @throws com.shop.auth.exception.BusinessException if the status is NEW or DELETED (system-managed),
     *         or if the admin is trying to suspend themselves
     */
    UserDto updateUserStatus(Long userId, UpdateUserStatusRequestDto request, Long requestingAdminId);

    /**
     * Soft-deletes a user by setting their status to DELETED and invalidating all active tokens.
     * This action is irreversible via the API. Admins cannot delete themselves.
     *
     * @param requestingAdminId the ID of the admin making the request (self-action guard)
     * @throws com.shop.auth.exception.ResourceNotFoundException if the user does not exist
     * @throws com.shop.auth.exception.BusinessException if the admin attempts self-deletion
     */
    void adminDeleteUser(Long userId, Long requestingAdminId);
}
