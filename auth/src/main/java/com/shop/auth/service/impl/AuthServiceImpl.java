package com.shop.auth.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.auth.dto.AddressDto;
import com.shop.auth.dto.LoginRequestDto;
import com.shop.auth.dto.LoginResponseDto;
import com.shop.auth.dto.RegisterRequestDto;
import com.shop.auth.dto.ResendOtpRequestDto;
import com.shop.auth.dto.UserDto;
import com.shop.auth.dto.VerifyOtpRequestDto;
import com.shop.auth.entity.Address;
import com.shop.auth.entity.User;
import com.shop.auth.entity.UserLog;
import com.shop.auth.exception.AccountLockedException;
import com.shop.auth.exception.EmailAlreadyExistsException;
import com.shop.auth.exception.InvalidCredentialsException;
import com.shop.auth.exception.UserNotActiveException;
import com.shop.auth.repository.UserLogRepository;
import com.shop.auth.repository.UserRepository;
import com.shop.auth.service.AuthService;
import com.shop.auth.service.JwtService;
import com.shop.auth.service.OtpService;
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

    private final UserRepository    userRepository;
    private final UserLogRepository userLogRepository;
    private final PasswordEncoder   passwordEncoder;
    private final JwtService        jwtService;
    private final OtpService        otpService;

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
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.NEW);
        user.setRole(request.getRole() != null ? request.getRole() : Role.USER);

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

        // Step 5 — record login time and reset failure counters if needed; always persist
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
        userLog.setUserToken(sha256Hex(token));  // store hash, not raw token
        userLog.setTokenType(tokenType);
        userLog.setIssuedAt(LocalDateTime.now());
        userLog.setExpiresAt(expiresAt);
        userLogRepository.save(userLog);
    }

    private static String sha256Hex(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
