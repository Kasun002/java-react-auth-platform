package com.shop.auth.service.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.shop.auth.dto.AddressDto;
import com.shop.auth.dto.ChangePasswordRequestDto;
import com.shop.auth.dto.ForgotPasswordRequestDto;
import com.shop.auth.dto.LoginRequestDto;
import com.shop.auth.dto.LoginResponseDto;
import com.shop.auth.dto.RefreshTokenRequestDto;
import com.shop.auth.dto.RefreshTokenResponseDto;
import com.shop.auth.dto.RegisterRequestDto;
import com.shop.auth.dto.ResendOtpRequestDto;
import com.shop.auth.dto.ResetPasswordRequestDto;
import com.shop.auth.dto.UserDto;
import com.shop.auth.dto.VerifyOtpRequestDto;
import com.shop.auth.entity.Address;
import com.shop.auth.entity.User;
import com.shop.auth.entity.UserLog;
import com.shop.auth.exception.AccountLockedException;
import com.shop.auth.exception.EmailAlreadyExistsException;
import com.shop.auth.exception.InvalidCredentialsException;
import com.shop.auth.exception.InvalidTokenException;
import com.shop.auth.exception.PasswordExpiredException;
import com.shop.auth.exception.PasswordResetTokenException;
import com.shop.auth.exception.UserNotActiveException;
import com.shop.auth.repository.UserGroupRepository;
import com.shop.auth.repository.UserLogRepository;
import com.shop.auth.repository.UserRepository;
import com.shop.auth.service.AuthService;
import com.shop.auth.service.EmailService;
import com.shop.auth.service.JwtService;
import com.shop.auth.service.OtpService;
import com.shop.auth.service.PasswordPolicyService;
import com.shop.auth.service.TokenBlacklistService;
import com.shop.auth.utils.HashUtil;
import com.shop.auth.utils.MaskingUtil;
import com.shop.auth.utils.Role;
import com.shop.auth.utils.TokenType;
import com.shop.auth.utils.UserStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int    MAX_FAILED_ATTEMPTS  = 5;
    private static final int    LOCKOUT_DURATION_MIN = 30;

    /**
     * Pre-computed BCrypt hash used for the timing-safe dummy password check
     * when a user is not found. Ensures user-not-found and wrong-password paths
     * take the same amount of time, preventing user-enumeration via timing.
     */
    private static final String DUMMY_BCRYPT_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    @Value("${app.security.password.max-age-days:90}")
    private int passwordMaxAgeDays;

    @Value("${app.jwt.refresh-token-expiry-ms:604800000}")
    private long refreshTokenExpiryMs;

    @Value("${app.password-reset.token-ttl-minutes:15}")
    private int passwordResetTokenTtlMinutes;

    @Value("${app.password-reset.base-url}")
    private String passwordResetBaseUrl;

    private static final String RESET_TOKEN_PREFIX = "reset:token:";

    private final UserRepository        userRepository;
    private final UserGroupRepository   userGroupRepository;
    private final UserLogRepository     userLogRepository;
    private final PasswordEncoder       passwordEncoder;
    private final JwtService            jwtService;
    private final OtpService            otpService;
    private final TokenBlacklistService tokenBlacklistService;
    private final PasswordPolicyService passwordPolicyService;
    private final EmailService          emailService;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    // ── Register ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void register(RegisterRequestDto request) {
        log.debug("Checking email uniqueness for email=[{}]", MaskingUtil.maskEmail(request.getEmail()));
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration rejected — email already exists: [{}]", MaskingUtil.maskEmail(request.getEmail()));
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        log.debug("Building user entity for email=[{}] role=[{}]",
                MaskingUtil.maskEmail(request.getEmail()), request.getRole());

        User user = new User();
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(encodedPassword);
        user.setStatus(UserStatus.NEW);
        user.setRole(request.getRole() != null ? request.getRole() : Role.USER);
        user.setPasswordChangedAt(LocalDateTime.now());

        List<Address> addresses = request.getAddresses().stream()
                .map(dto -> {
                    Address address = new Address();
                    address.setAddressLine1(dto.getAddressLine1());
                    address.setAddressLine2(dto.getAddressLine2());
                    address.setStreet(dto.getStreet());
                    address.setPostalCode(dto.getPostalCode());
                    address.setState(dto.getState());
                    address.setCountry(dto.getCountry());
                    address.setUser(user);
                    return address;
                })
                .collect(Collectors.toList());

        user.getAddresses().addAll(addresses);

        log.debug("Persisting user with [{}] address(es)", addresses.size());
        userRepository.save(user);
        log.info("User persisted successfully — email=[{}] status=[{}] role=[{}]",
                MaskingUtil.maskEmail(user.getEmail()), user.getStatus(), user.getRole());

        // Seed password history so future change-password can enforce no-reuse from day one
        passwordPolicyService.recordPasswordChange(user, encodedPassword);

        // Auto-assign new users to the default RETAIL_CUSTOMER group (PCI-DSS Req 7.2 — least privilege)
        userGroupRepository.findByName("RETAIL_CUSTOMER").ifPresentOrElse(
                group -> {
                    user.getGroups().add(group);
                    userRepository.save(user);
                    log.info("Auto-assigned RETAIL_CUSTOMER group to email=[{}]",
                            MaskingUtil.maskEmail(user.getEmail()));
                },
                () -> log.warn("RETAIL_CUSTOMER group not found — user registered without default group: email=[{}]",
                        MaskingUtil.maskEmail(user.getEmail()))
        );

        // OTP runs in REQUIRES_NEW — failure does not roll back the committed user record
        try {
            otpService.generateAndSend(user);
        } catch (Exception e) {
            log.error("OTP send failed for email=[{}]. User registered; must use /auth/resend-otp.",
                    MaskingUtil.maskEmail(user.getEmail()), e);
        }
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    /**
     * noRollbackFor ensures failed-attempt counters and lockout timestamps are
     * committed even when a BusinessException is thrown mid-flow.
     */
    @Override
    @Transactional(noRollbackFor = com.shop.auth.exception.BusinessException.class)
    public LoginResponseDto login(LoginRequestDto request) {
        log.debug("Login attempt for email=[{}]", MaskingUtil.maskEmail(request.getUsername()));

        // Step 1 — look up user, equalize timing if not found (prevents user enumeration)
        Optional<User> optionalUser = userRepository.findByEmail(request.getUsername());
        if (optionalUser.isEmpty()) {
            passwordEncoder.matches(request.getPassword(), DUMMY_BCRYPT_HASH); // timing equalizer
            log.warn("Login failed — user not found: [{}]", MaskingUtil.maskEmail(request.getUsername()));
            throw new InvalidCredentialsException();
        }
        User user = optionalUser.get();

        // Step 2 — check active lockout before touching the password
        if (user.getLockedUntil() != null && LocalDateTime.now().isBefore(user.getLockedUntil())) {
            log.warn("Login rejected — account locked: email=[{}] until=[{}]",
                    MaskingUtil.maskEmail(user.getEmail()), user.getLockedUntil());
            throw new AccountLockedException(user.getLockedUntil());
        }

        // Step 3 — verify password; track failed attempts and lock if threshold breached
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            recordFailedAttempt(user);
            log.warn("Login failed — bad credentials: email=[{}] attempts=[{}]",
                    MaskingUtil.maskEmail(user.getEmail()), user.getFailedLoginAttempts());
            throw new InvalidCredentialsException();
        }

        // Step 4 — account must be ACTIVE (checked after password to avoid leaking account state)
        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Login rejected — account not active: email=[{}] status=[{}]",
                    MaskingUtil.maskEmail(user.getEmail()), user.getStatus());
            throw new UserNotActiveException(user.getStatus());
        }

        // Step 5 — enforce password age (PCI-DSS Req 8.3.9 — max 90 days)
        if (user.getPasswordChangedAt() != null
                && user.getPasswordChangedAt().isBefore(LocalDateTime.now().minusDays(passwordMaxAgeDays))) {
            log.warn("Login rejected — password expired: email=[{}] passwordChangedAt=[{}]",
                    MaskingUtil.maskEmail(user.getEmail()), user.getPasswordChangedAt());
            throw new PasswordExpiredException(passwordMaxAgeDays);
        }

        // Step 6 — record login time and reset failure counters if needed; always persist
        user.setLastLoginAt(LocalDateTime.now());
        if (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
        }
        userRepository.save(user);

        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        persistUserLog(user, accessToken,  TokenType.ACCESS);
        persistUserLog(user, refreshToken, TokenType.REFRESH);

        log.info("Login successful — email=[{}] role=[{}]",
                MaskingUtil.maskEmail(user.getEmail()), user.getRole());

        LoginResponseDto response = new LoginResponseDto();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setUser(buildUserDto(user));
        return response;
    }

    // ── Change password ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public void changePassword(String accessToken, ChangePasswordRequestDto request) {
        Long userId = jwtService.extractUserId(accessToken);
        User user = userRepository.findById(userId)
                .orElseThrow(InvalidCredentialsException::new);

        log.info("Change-password initiated — email=[{}]", MaskingUtil.maskEmail(user.getEmail()));

        // Step 1 — verify the current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("Change-password rejected — wrong current password: email=[{}]",
                    MaskingUtil.maskEmail(user.getEmail()));
            throw new InvalidCredentialsException();
        }

        // Step 2 — enforce history: new password must not match any recent passwords
        passwordPolicyService.enforceHistory(user, request.getNewPassword());

        // Step 3 — encode and save the new password
        String encodedNewPassword = passwordEncoder.encode(request.getNewPassword());
        user.setPassword(encodedNewPassword);
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        // Step 4 — record new password in history
        passwordPolicyService.recordPasswordChange(user, encodedNewPassword);

        // Step 5 — invalidate all tokens for this user on all devices
        long ttlSeconds = refreshTokenExpiryMs / 1000;
        tokenBlacklistService.invalidateAllUserTokens(userId, ttlSeconds);

        log.info("Password changed successfully — email=[{}] all sessions invalidated",
                MaskingUtil.maskEmail(user.getEmail()));
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    /**
     * Exchanges a valid refresh token for a new access + refresh token pair.
     *
     * <p>Validation order (each step fails fast with the same generic 401 to
     * prevent information leakage):</p>
     * <ol>
     *   <li>Signature and expiry — rejects tampered or expired tokens.</li>
     *   <li>Token type must be REFRESH — rejects access tokens used as refresh tokens.</li>
     *   <li>JTI blacklist check — rejects already-used or revoked refresh tokens.
     *       A hit here indicates possible token theft; the caller should re-authenticate.</li>
     *   <li>User must still be ACTIVE — rejects suspended accounts.</li>
     * </ol>
     *
     * <p>Refresh token rotation: the supplied refresh token is blacklisted immediately
     * after the new pair is generated. This ensures each refresh token can be used
     * exactly once, limiting the window for replay attacks.</p>
     */
    @Override
    @Transactional
    public RefreshTokenResponseDto refresh(RefreshTokenRequestDto request) {
        String token = request.getRefreshToken();

        // Step 1 — validate signature and expiry
        if (!jwtService.isTokenValid(token)) {
            log.warn("Refresh rejected — invalid or expired token");
            throw new InvalidTokenException();
        }

        // Step 2 — must be a REFRESH token (block access tokens from being used here)
        if (!TokenType.REFRESH.name().equals(jwtService.extractTokenType(token))) {
            log.warn("Refresh rejected — wrong token type");
            throw new InvalidTokenException();
        }

        // Step 3 — check blacklist (already-used or explicitly revoked)
        String jti = jwtService.extractJti(token);
        if (tokenBlacklistService.isBlacklisted(jti)) {
            log.warn("Refresh rejected — token already revoked (possible replay attack): jti=[{}]", jti);
            throw new InvalidTokenException();
        }

        // Step 4 — load user and verify account is still active
        String email = jwtService.extractUsername(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Refresh rejected — user not found: email=[{}]", MaskingUtil.maskEmail(email));
                    return new InvalidTokenException();
                });

        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Refresh rejected — account not active: email=[{}] status=[{}]",
                    MaskingUtil.maskEmail(email), user.getStatus());
            throw new InvalidTokenException();
        }

        // Issue new token pair BEFORE blacklisting the old refresh token
        String newAccessToken  = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        persistUserLog(user, newAccessToken,  TokenType.ACCESS);
        persistUserLog(user, newRefreshToken, TokenType.REFRESH);

        // Rotate: blacklist the consumed refresh token so it cannot be reused
        revokeToken(token, "refresh");

        log.info("Token refresh successful — email=[{}]", MaskingUtil.maskEmail(email));
        return new RefreshTokenResponseDto(newAccessToken, newRefreshToken);
    }

    // ── OTP verification ──────────────────────────────────────────────────────

    @Override
    public void verifyOtp(VerifyOtpRequestDto request) {
        log.debug("OTP verification delegated for email=[{}]", MaskingUtil.maskEmail(request.getEmail()));
        otpService.verify(request.getEmail(), request.getOtp());
    }

    @Override
    public void resendOtp(ResendOtpRequestDto request) {
        log.debug("OTP resend delegated for email=[{}]", MaskingUtil.maskEmail(request.getEmail()));
        otpService.resend(request.getEmail());
    }

    // ── Forgot / reset password ───────────────────────────────────────────────

    /**
     * Generates a single-use reset token, stores its SHA-256 hash in Redis with a
     * short TTL, and sends a reset link to the user's email.
     *
     * <p>Always returns the same response regardless of whether the email exists,
     * to prevent account enumeration attacks.</p>
     */
    @Override
    public void forgotPassword(ForgotPasswordRequestDto request) {
        String email = request.getEmail();
        log.info("Forgot-password request for email=[{}]", MaskingUtil.maskEmail(email));

        userRepository.findByEmail(email).ifPresent(user -> {
            // Generate a cryptographically random token — never stored in plain text
            String rawToken   = UUID.randomUUID().toString();
            String tokenHash  = HashUtil.sha256Hex(rawToken);

            long ttlSeconds = (long) passwordResetTokenTtlMinutes * 60;
            redisTemplate.opsForValue()
                    .set(RESET_TOKEN_PREFIX + tokenHash,
                         String.valueOf(user.getId()),
                         ttlSeconds, TimeUnit.SECONDS);

            String resetLink = passwordResetBaseUrl + "?token=" + rawToken;
            try {
                emailService.sendPasswordResetEmail(
                        user.getEmail(), user.getName(), resetLink, passwordResetTokenTtlMinutes);
            } catch (Exception e) {
                log.error("Failed to send password reset email to=[{}]: {}",
                        MaskingUtil.maskEmail(email), e.getMessage());
            }
        });

        // Log at info level whether user was found or not — same response to caller either way
        log.info("Forgot-password flow completed for email=[{}]", MaskingUtil.maskEmail(email));
    }

    /**
     * Validates the reset token, enforces password history, saves the new password,
     * deletes the token (single-use), and invalidates all active sessions.
     */
    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequestDto request) {
        String tokenHash = HashUtil.sha256Hex(request.getToken());
        String userIdStr = redisTemplate.opsForValue().get(RESET_TOKEN_PREFIX + tokenHash);

        if (userIdStr == null) {
            log.warn("Reset-password failed — token not found or expired");
            throw new PasswordResetTokenException();
        }

        Long userId = Long.parseLong(userIdStr);
        User user = userRepository.findById(userId)
                .orElseThrow(PasswordResetTokenException::new);

        log.info("Reset-password proceeding for email=[{}]", MaskingUtil.maskEmail(user.getEmail()));

        // Enforce history before consuming the token — fail fast without side effects
        passwordPolicyService.enforceHistory(user, request.getNewPassword());

        // Consume the token immediately (single-use)
        redisTemplate.delete(RESET_TOKEN_PREFIX + tokenHash);

        String encodedNewPassword = passwordEncoder.encode(request.getNewPassword());
        user.setPassword(encodedNewPassword);
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        passwordPolicyService.recordPasswordChange(user, encodedNewPassword);

        // Invalidate all active sessions — user must log in with the new password
        long ttlSeconds = refreshTokenExpiryMs / 1000;
        tokenBlacklistService.invalidateAllUserTokens(userId, ttlSeconds);

        log.info("Password reset successful — all sessions invalidated: email=[{}]",
                MaskingUtil.maskEmail(user.getEmail()));
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    /**
     * Revokes the session by blacklisting both the access and refresh token JTIs
     * in Redis.  Each entry is stored with the token's remaining TTL so Redis
     * self-expires the entry — the blacklist never grows unbounded.
     *
     * <p>Each token is revoked independently; failure to revoke one does not
     * prevent revoking the other.  A missing or invalid refresh token is logged
     * as a warning but does not cause the call to fail — the access token is
     * always revoked.</p>
     */
    @Override
    public void logout(String accessToken, String refreshToken) {
        String email = extractEmailSafe(accessToken);
        log.info("Logout initiated — email=[{}]", MaskingUtil.maskEmail(email));

        revokeToken(accessToken, "access");

        if (refreshToken != null && !refreshToken.isBlank()) {
            revokeToken(refreshToken, "refresh");
        } else {
            log.warn("Logout called without refresh token — refresh token not revoked: email=[{}]",
                    MaskingUtil.maskEmail(email));
        }

        log.info("Logout completed — both tokens revoked: email=[{}]", MaskingUtil.maskEmail(email));
    }

    /**
     * Extracts the JTI from the token, computes its remaining lifetime in seconds,
     * and adds it to the Redis blacklist.  If the token is already expired the TTL
     * will be &le; 0 and {@link TokenBlacklistService#blacklist} will be a no-op.
     */
    private void revokeToken(String token, String tokenLabel) {
        try {
            String jti        = jwtService.extractJti(token);
            long   ttlSeconds = jwtService.extractExpiration(token).toInstant().getEpochSecond()
                                - Instant.now().getEpochSecond();
            tokenBlacklistService.blacklist(jti, ttlSeconds);
            log.debug("Token revoked — type=[{}] jti=[{}] ttlSeconds=[{}]", tokenLabel, jti, ttlSeconds);
        } catch (Exception e) {
            log.warn("Failed to revoke {} token during logout: {}", tokenLabel, e.getMessage());
        }
    }

    private String extractEmailSafe(String token) {
        try {
            return jwtService.extractUsername(token);
        } catch (Exception e) {
            return "unknown";
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void recordFailedAttempt(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MIN);
            user.setLockedUntil(lockUntil);
            log.warn("Account locked: email=[{}] attempts=[{}] lockedUntil=[{}]",
                    MaskingUtil.maskEmail(user.getEmail()), attempts, lockUntil);
        }
        userRepository.save(user);
    }

    private UserDto buildUserDto(User user) {
        List<AddressDto> addressDtos = user.getAddresses().stream()
                .map(a -> {
                    AddressDto dto = new AddressDto();
                    dto.setAddressLine1(a.getAddressLine1());
                    dto.setAddressLine2(a.getAddressLine2());
                    dto.setStreet(a.getStreet());
                    dto.setPostalCode(a.getPostalCode());
                    dto.setState(a.getState());
                    dto.setCountry(a.getCountry());
                    return dto;
                })
                .collect(Collectors.toList());

        // RBAC fields — computed from group/role membership (empty until Step 4 assigns groups)
        List<String> groupNames = user.getGroups().stream()
                .map(g -> g.getName())
                .collect(Collectors.toList());

        Set<String> roleNames = new LinkedHashSet<>();
        user.getGroups().forEach(g -> g.getRoles().forEach(r -> roleNames.add(r.getName())));
        user.getDirectRoles().forEach(r -> roleNames.add(r.getName()));

        Set<String> permCodes = new LinkedHashSet<>();
        user.getGroups().forEach(g ->
                g.getRoles().forEach(r ->
                        r.getPermissions().forEach(p -> permCodes.add(p.getCode()))));
        user.getDirectRoles().forEach(r ->
                r.getPermissions().forEach(p -> permCodes.add(p.getCode())));

        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setStatus(user.getStatus());
        dto.setRole(user.getRole());
        dto.setDateOfBirth(user.getDateOfBirth());
        dto.setGender(user.getGender());
        dto.setProfilePictureUrl(user.getProfilePictureUrl());
        dto.setLastLoginAt(user.getLastLoginAt());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setGroups(groupNames);
        dto.setRoles(new ArrayList<>(roleNames));
        dto.setEffectivePermissions(new ArrayList<>(permCodes));
        dto.setAddresses(addressDtos);
        return dto;
    }

    private void persistUserLog(User user, String token, TokenType tokenType) {
        LocalDateTime expiresAt = jwtService.extractExpiration(token)
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        UserLog userLog = new UserLog();
        userLog.setUser(user);
        userLog.setUserToken(HashUtil.sha256Hex(token));  // store hash, not raw token
        userLog.setTokenType(tokenType);
        userLog.setIssuedAt(LocalDateTime.now());
        userLog.setExpiresAt(expiresAt);
        userLog.setIpAddress(extractClientIp());
        userLog.setUserAgent(extractUserAgent());
        userLogRepository.save(userLog);
    }

    /**
     * Extracts the real client IP, honouring the {@code X-Forwarded-For} header
     * set by load balancers and reverse proxies.
     */
    private String extractClientIp() {
        HttpServletRequest request = currentRequest();
        if (request == null) return null;
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim(); // first entry is the originating client
        }
        return request.getRemoteAddr();
    }

    private String extractUserAgent() {
        HttpServletRequest request = currentRequest();
        if (request == null) return null;
        String ua = request.getHeader("User-Agent");
        return (ua != null && ua.length() > 512) ? ua.substring(0, 512) : ua;
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

}
