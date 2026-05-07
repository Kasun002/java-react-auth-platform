package com.shop.auth.service.impl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.shop.auth.config.AdAuthProperties;
import com.shop.auth.dto.AddressDto;
import com.shop.auth.dto.AdLoginRequestDto;
import com.shop.auth.dto.LoginResponseDto;
import com.shop.auth.dto.UserDto;
import com.shop.auth.entity.User;
import com.shop.auth.entity.UserGroup;
import com.shop.auth.entity.UserLog;
import com.shop.auth.exception.AdAuthenticationException;
import com.shop.auth.repository.UserLogRepository;
import com.shop.auth.repository.UserRepository;
import com.shop.auth.service.AdAuthService;
import com.shop.auth.service.AdGroupMappingService;
import com.shop.auth.service.AdLdapGroupService;
import com.shop.auth.service.JwtService;
import com.shop.auth.utils.AuthProvider;
import com.shop.auth.utils.HashUtil;
import com.shop.auth.utils.MaskingUtil;
import com.shop.auth.utils.Role;
import com.shop.auth.utils.TokenType;
import com.shop.auth.utils.UserStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Validates an Azure AD OIDC ID token, provisions / syncs the user, and issues
 * this service's own JWT access + refresh token pair.
 *
 * <h3>Login flow</h3>
 * <ol>
 *   <li>Check {@code app.ad.enabled} — reject with 503 if disabled.</li>
 *   <li>Decode and validate the ID token signature via JWKS, verify iss + aud.</li>
 *   <li>Extract {@code oid} claim as the AD object ID; fall back to {@code sub}.</li>
 *   <li>Find or provision the local User record.</li>
 *   <li>Fetch LDAP groups; resolve to local UserGroups via AdGroupMappingService.</li>
 *   <li>Replace the user's current group memberships with the resolved set.</li>
 *   <li>Issue JWT pair; persist audit log; return LoginResponseDto.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdAuthServiceImpl implements AdAuthService {

    private final AdAuthProperties      props;
    private final UserRepository        userRepository;
    private final UserLogRepository     userLogRepository;
    private final JwtService            jwtService;
    private final AdLdapGroupService    ldapGroupService;
    private final AdGroupMappingService groupMappingService;

    /** Built lazily in {@code @PostConstruct} when AD is enabled. */
    private JwtDecoder jwtDecoder;

    @PostConstruct
    void init() {
        if (!props.isEnabled()) {
            log.info("AD login disabled (app.ad.enabled=false) — NimbusJwtDecoder not initialised");
            return;
        }

        if (props.getJwksUri() == null || props.getJwksUri().isBlank()) {
            log.warn("AD login enabled but app.ad.jwks-uri is blank — AD login will fail at runtime");
            return;
        }

        jwtDecoder = NimbusJwtDecoder.withJwkSetUri(props.getJwksUri()).build();
        log.info("AD login enabled — JWKS URI=[{}] issuer=[{}] audience=[{}]",
                props.getJwksUri(), props.getIssuer(), props.getAudience());
    }

    // ── Main entry point ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public LoginResponseDto adLogin(AdLoginRequestDto request) {
        if (!props.isEnabled()) {
            throw new AdAuthenticationException("AD login is not enabled on this server");
        }

        // Step 1: validate the token
        Jwt jwt = validateToken(request.getIdToken());
        log.debug("AD ID token validated — subject=[{}]", jwt.getSubject());

        // Step 2: extract identity claims
        String adObjectId = extractAdObjectId(jwt);
        String email      = jwt.getClaimAsString("email");
        String upn        = jwt.getClaimAsString("upn");           // Azure AD UPN
        String preferred  = jwt.getClaimAsString("preferred_username"); // Keycloak
        String name       = jwt.getClaimAsString("name");

        String userEmail = firstNonBlank(email, upn, preferred);
        if (userEmail == null) {
            throw new AdAuthenticationException("ID token missing email / upn / preferred_username claim");
        }
        String displayName = name != null ? name : userEmail;

        log.debug("AD login: email=[{}] adObjectId=[{}]", MaskingUtil.maskEmail(userEmail), adObjectId);

        // Step 3: find or provision the local user
        User user = findOrProvisionUser(adObjectId, userEmail, displayName);

        // Step 4: account must not be locked / explicitly disabled
        if (user.getStatus() == UserStatus.INACTIVE || user.getStatus() == UserStatus.DELETED) {
            throw new AdAuthenticationException(
                    "account is " + user.getStatus().name().toLowerCase());
        }

        // Step 5: sync LDAP groups
        syncGroups(user, userEmail);

        // Step 6: record login time
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Step 7: issue JWT pair
        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        persistUserLog(user, accessToken,  TokenType.ACCESS);
        persistUserLog(user, refreshToken, TokenType.REFRESH);

        log.info("AD login successful — email=[{}]", MaskingUtil.maskEmail(userEmail));

        LoginResponseDto response = new LoginResponseDto();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setUser(buildUserDto(user));
        return response;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Jwt validateToken(String rawToken) {
        if (jwtDecoder == null) {
            throw new AdAuthenticationException("JWKS URI not configured — cannot validate ID token");
        }
        try {
            Jwt jwt = jwtDecoder.decode(rawToken);

            // Validate issuer if configured
            if (props.getIssuer() != null && !props.getIssuer().isBlank()) {
                String iss = jwt.getIssuer() != null ? jwt.getIssuer().toString() : null;
                if (!props.getIssuer().equals(iss)) {
                    throw new AdAuthenticationException("issuer mismatch: expected ["
                            + props.getIssuer() + "] got [" + iss + "]");
                }
            }

            // Validate audience if configured
            if (props.getAudience() != null && !props.getAudience().isBlank()) {
                List<String> audiences = jwt.getAudience();
                if (audiences == null || !audiences.contains(props.getAudience())) {
                    throw new AdAuthenticationException(
                            "audience mismatch — token does not contain audience: " + props.getAudience());
                }
            }

            return jwt;

        } catch (JwtException e) {
            log.warn("AD ID token validation failed: {}", e.getMessage());
            throw new AdAuthenticationException("invalid or expired ID token");
        }
    }

    private String extractAdObjectId(Jwt jwt) {
        // Azure AD uses "oid" (Object ID); Keycloak uses "sub"
        String oid = jwt.getClaimAsString("oid");
        return oid != null ? oid : jwt.getSubject();
    }

    /**
     * Finds an existing user by AD Object ID, or by email (handles re-provisioning),
     * or creates a brand-new user for first-time AD login.
     */
    private User findOrProvisionUser(String adObjectId, String email, String displayName) {
        // 1. Try by AD Object ID (most reliable — survives email renames)
        Optional<User> byOid = userRepository.findByAdObjectId(adObjectId);
        if (byOid.isPresent()) {
            User u = byOid.get();
            // Keep email and name in sync with the directory
            u.setEmail(email);
            u.setName(displayName);
            return userRepository.save(u);
        }

        // 2. Try by email — user may have been pre-provisioned locally
        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            User u = byEmail.get();
            // Adopt the AD identity
            u.setAdObjectId(adObjectId);
            u.setAuthProvider(AuthProvider.AZURE_AD);
            u.setName(displayName);
            log.info("AD login: linked existing local user to AD — email=[{}]",
                    MaskingUtil.maskEmail(email));
            return userRepository.save(u);
        }

        // 3. First-time AD login — create a new user
        log.info("AD login: provisioning new user — email=[{}]", MaskingUtil.maskEmail(email));
        User user = new User();
        user.setName(displayName);
        user.setEmail(email);
        user.setAdObjectId(adObjectId);
        user.setAuthProvider(AuthProvider.AZURE_AD);
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(Role.USER);
        // Random unguessable password — local login is blocked for AZURE_AD users
        user.setPassword("$AD$" + UUID.randomUUID());
        user.setPasswordChangedAt(LocalDateTime.now()); // no password age enforcement for AD users; column is NOT NULL
        return userRepository.save(user);
    }

    /** Replace current group memberships with the LDAP-sourced set. */
    private void syncGroups(User user, String userEmail) {
        List<AdLdapGroupService.LdapGroup> ldapGroups = ldapGroupService.getGroupsForUser(userEmail);

        if (ldapGroups.isEmpty()) {
            log.debug("No LDAP groups returned for email=[{}] — group membership unchanged",
                    MaskingUtil.maskEmail(userEmail));
            // On first login with no LDAP groups, assign the default group
            if (user.getGroups().isEmpty()) {
                groupMappingService.resolveLocalGroups(List.of())
                        .forEach(g -> user.getGroups().add(g));
            }
            return;
        }

        Set<UserGroup> resolvedGroups = groupMappingService.resolveLocalGroups(ldapGroups);
        user.getGroups().clear();
        user.getGroups().addAll(resolvedGroups);
        log.debug("Synced {} local group(s) for email=[{}]",
                resolvedGroups.size(), MaskingUtil.maskEmail(userEmail));
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

        List<String> groupNames = user.getGroups().stream()
                .map(UserGroup::getName)
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
        dto.setAuthProvider(user.getAuthProvider());
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
        userLog.setUserToken(HashUtil.sha256Hex(token));
        userLog.setTokenType(tokenType);
        userLog.setIssuedAt(LocalDateTime.now());
        userLog.setExpiresAt(expiresAt);
        userLog.setIpAddress(extractClientIp());
        userLog.setUserAgent(extractUserAgent());
        userLogRepository.save(userLog);
    }

    private String extractClientIp() {
        HttpServletRequest request = currentRequest();
        if (request == null) return null;
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
    }

    private String extractUserAgent() {
        HttpServletRequest request = currentRequest();
        return (request != null) ? request.getHeader("User-Agent") : null;
    }

    private HttpServletRequest currentRequest() {
        try {
            return ((ServletRequestAttributes)
                    RequestContextHolder.currentRequestAttributes()).getRequest();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
