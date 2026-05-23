package com.org.auth.service.impl;

import com.org.auth.config.AdAuthProperties;
import com.org.auth.config.AdAuthProperties.UnmappedGroupStrategy;
import com.org.auth.dto.AdLoginRequestDto;
import com.org.auth.dto.LoginResponseDto;
import com.org.auth.entity.Address;
import com.org.auth.entity.User;
import com.org.auth.entity.UserGroup;
import com.org.auth.exception.AdAuthenticationException;
import com.org.auth.repository.UserLogRepository;
import com.org.auth.repository.UserRepository;
import com.org.auth.service.AdGroupMappingService;
import com.org.auth.service.AdLdapGroupService;
import com.org.auth.service.AdLdapGroupService.LdapGroup;
import com.org.auth.service.JwtService;
import com.org.auth.utils.AuthProvider;
import com.org.auth.utils.TokenType;
import com.org.auth.utils.UserStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AdAuthServiceImpl}.
 *
 * <p>The {@link JwtDecoder} is injected via a package-private field setter
 * (reflection helper in {@link #setDecoder(JwtDecoder)}) so tests can supply a
 * mock without going through the real NimbusJwtDecoder / JWKS URI. This avoids
 * any network calls or Spring context overhead.
 *
 * <p>All repository and service collaborators are Mockito mocks.
 * Strictness is STRICT_STUBS so unused stubs fail fast.
 */
@DisplayName("AdAuthServiceImpl")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class AdAuthServiceImplTest {

    // ── Mocked collaborators ──────────────────────────────────────────────────

    @Mock private AdAuthProperties    props;
    @Mock private UserRepository      userRepository;
    @Mock private UserLogRepository   userLogRepository;
    @Mock private JwtService          jwtService;
    @Mock private AdLdapGroupService  ldapGroupService;
    @Mock private AdGroupMappingService groupMappingService;
    @Mock private JwtDecoder          jwtDecoder;

    @InjectMocks
    private AdAuthServiceImpl service;

    // ── Constants ─────────────────────────────────────────────────────────────

    private static final String RAW_TOKEN      = "eyRaw.ID.token";
    private static final String ACCESS_TOKEN   = "eyAccess.jwt";
    private static final String REFRESH_TOKEN  = "eyRefresh.jwt";
    private static final String AD_OBJECT_ID   = "oid-uuid-1234";
    private static final String USER_EMAIL     = "alice@corp.example.com";
    private static final String DISPLAY_NAME   = "Alice Smith";

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Inject the mock JwtDecoder into the non-final field before every test.
     * Mockito @InjectMocks uses constructor injection (from @RequiredArgsConstructor)
     * which only covers final fields — the jwtDecoder field is set by @PostConstruct
     * at runtime but @PostConstruct is never invoked in unit tests.
     */
    @BeforeEach
    void injectDecoder() throws Exception {
        setDecoder(jwtDecoder);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Injects the mock JwtDecoder into the package-private field via reflection,
     * bypassing {@code @PostConstruct init()} which would hit a real JWKS URI.
     */
    private void setDecoder(JwtDecoder decoder) throws Exception {
        var field = AdAuthServiceImpl.class.getDeclaredField("jwtDecoder");
        field.setAccessible(true);
        field.set(service, decoder);
    }

    /**
     * Builds a minimal Jwt stub with the given claims.
     * Only claims relevant to the test need to be supplied — the rest default to
     * sensible values so Spring's Jwt record does not throw on construction.
     */
    private Jwt buildJwt(Map<String, Object> extraClaims) {
        Map<String, Object> headers  = Map.of("alg", "RS256", "typ", "JWT");
        Map<String, Object> baseClaims = new HashMap<>();
        baseClaims.put("sub",   AD_OBJECT_ID);
        baseClaims.put("iss",   "https://login.microsoftonline.com/tenant/v2.0");
        baseClaims.put("aud",   List.of("client-id-123"));
        baseClaims.put("iat",   Instant.now());
        baseClaims.put("exp",   Instant.now().plusSeconds(3600));
        baseClaims.putAll(extraClaims);
        return new Jwt(RAW_TOKEN, Instant.now(), Instant.now().plusSeconds(3600), headers, baseClaims);
    }

    /** Active, AZURE_AD user with no addresses and no group memberships. */
    private User activeAdUser(Long id) {
        User u = new User();
        u.setId(id);
        u.setName(DISPLAY_NAME);
        u.setEmail(USER_EMAIL);
        u.setAdObjectId(AD_OBJECT_ID);
        u.setAuthProvider(AuthProvider.AZURE_AD);
        u.setStatus(UserStatus.ACTIVE);
        u.setPassword("$AD$placeholder");
        u.setPasswordChangedAt(LocalDateTime.now());
        u.setGroups(new HashSet<>());
        u.setDirectRoles(new HashSet<>());
        u.setAddresses(new ArrayList<>());
        return u;
    }

    /**
     * Stubs the full happy-path collaborator calls so individual test methods
     * only need to override what they're testing.
     */
    private void stubHappyPath(User user) {
        when(props.isEnabled()).thenReturn(true);
        when(props.getIssuer()).thenReturn(null);   // skip issuer validation
        when(props.getAudience()).thenReturn(null);  // skip audience validation
        when(jwtDecoder.decode(RAW_TOKEN)).thenReturn(
                buildJwt(Map.of("oid", AD_OBJECT_ID, "email", USER_EMAIL, "name", DISPLAY_NAME)));

        when(userRepository.findByAdObjectId(AD_OBJECT_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        when(ldapGroupService.getGroupsForUser(USER_EMAIL)).thenReturn(List.of());
        // user already has groups → no default group assignment
        user.getGroups().add(new UserGroup());

        when(jwtService.generateAccessToken(user)).thenReturn(ACCESS_TOKEN);
        when(jwtService.generateRefreshToken(user)).thenReturn(REFRESH_TOKEN);
        when(jwtService.extractExpiration(any())).thenReturn(new Date(Instant.now().plusSeconds(900).toEpochMilli()));
        when(userLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Feature-flag guard
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("AD feature-flag guard")
    class FeatureFlagGuard {

        @Test
        @DisplayName("Throws AdAuthenticationException immediately when AD is disabled")
        void shouldRejectWhenAdDisabled() {
            when(props.isEnabled()).thenReturn(false);

            AdLoginRequestDto req = new AdLoginRequestDto();
            req.setIdToken(RAW_TOKEN);

            assertThatThrownBy(() -> service.adLogin(req))
                    .isInstanceOf(AdAuthenticationException.class)
                    .hasMessageContaining("not enabled");
        }

        @Test
        @DisplayName("Throws AdAuthenticationException when jwtDecoder is null (JWKS URI not configured)")
        void shouldRejectWhenJwtDecoderIsNull() throws Exception {
            when(props.isEnabled()).thenReturn(true);
            setDecoder(null); // override the @InjectMocks-injected mock

            AdLoginRequestDto req = new AdLoginRequestDto();
            req.setIdToken(RAW_TOKEN);

            assertThatThrownBy(() -> service.adLogin(req))
                    .isInstanceOf(AdAuthenticationException.class)
                    .hasMessageContaining("JWKS URI not configured");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Token validation
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Token validation — validateToken()")
    class TokenValidation {

        @BeforeEach
        void adEnabled() {
            when(props.isEnabled()).thenReturn(true);
        }

        @Test
        @DisplayName("Throws AdAuthenticationException when JwtDecoder rejects the token")
        void shouldThrowOnJwtException() {
            when(jwtDecoder.decode(RAW_TOKEN)).thenThrow(new JwtException("Expired"));

            AdLoginRequestDto req = new AdLoginRequestDto();
            req.setIdToken(RAW_TOKEN);

            assertThatThrownBy(() -> service.adLogin(req))
                    .isInstanceOf(AdAuthenticationException.class)
                    .hasMessageContaining("invalid or expired ID token");
        }

        @Test
        @DisplayName("Throws AdAuthenticationException with generic message on issuer mismatch — does not leak internal issuer config")
        void shouldThrowGenericMessageOnIssuerMismatch() {
            Jwt jwt = buildJwt(Map.of(
                    "iss", "https://rogue-idp.evil.com",
                    "email", USER_EMAIL));

            when(jwtDecoder.decode(RAW_TOKEN)).thenReturn(jwt);
            when(props.getIssuer()).thenReturn("https://login.microsoftonline.com/tenant/v2.0");

            AdLoginRequestDto req = new AdLoginRequestDto();
            req.setIdToken(RAW_TOKEN);

            assertThatThrownBy(() -> service.adLogin(req))
                    .isInstanceOf(AdAuthenticationException.class)
                    .hasMessageContaining("token issuer is not trusted")
                    // Critical: internal issuer URI must NOT appear in the exception message
                    .hasMessageNotContaining("login.microsoftonline.com");
        }

        @Test
        @DisplayName("Throws AdAuthenticationException on audience mismatch")
        void shouldThrowOnAudienceMismatch() {
            Jwt jwt = buildJwt(Map.of(
                    "aud",   List.of("wrong-client-id"),
                    "email", USER_EMAIL));

            when(jwtDecoder.decode(RAW_TOKEN)).thenReturn(jwt);
            when(props.getIssuer()).thenReturn(null);
            when(props.getAudience()).thenReturn("expected-client-id");

            AdLoginRequestDto req = new AdLoginRequestDto();
            req.setIdToken(RAW_TOKEN);

            assertThatThrownBy(() -> service.adLogin(req))
                    .isInstanceOf(AdAuthenticationException.class)
                    .hasMessageContaining("audience mismatch");
        }

        @Test
        @DisplayName("Passes validation when issuer and audience props are null (validation disabled)")
        void shouldSkipValidationWhenPropsAreNull() throws Exception {
            User user = activeAdUser(1L);
            stubHappyPath(user);

            AdLoginRequestDto req = new AdLoginRequestDto();
            req.setIdToken(RAW_TOKEN);

            LoginResponseDto result = service.adLogin(req);
            assertThat(result.getAccessToken()).isEqualTo(ACCESS_TOKEN);
        }

        @Test
        @DisplayName("Throws AdAuthenticationException when all email/upn/preferred_username claims are absent")
        void shouldThrowWhenNoEmailClaimPresent() {
            Jwt jwt = buildJwt(Map.of("oid", AD_OBJECT_ID)); // no email claims

            when(jwtDecoder.decode(RAW_TOKEN)).thenReturn(jwt);
            when(props.getIssuer()).thenReturn(null);
            when(props.getAudience()).thenReturn(null);

            AdLoginRequestDto req = new AdLoginRequestDto();
            req.setIdToken(RAW_TOKEN);

            assertThatThrownBy(() -> service.adLogin(req))
                    .isInstanceOf(AdAuthenticationException.class)
                    .hasMessageContaining("email");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Claim extraction — email / OID priority
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Claim extraction — email and OID priority")
    class ClaimExtraction {

        @Test
        @DisplayName("Uses 'email' claim as primary email source when present")
        void shouldUseEmailClaimFirst() throws Exception {
            Jwt jwt = buildJwt(Map.of(
                    "oid",                AD_OBJECT_ID,
                    "email",              USER_EMAIL,
                    "upn",                "upn@corp.example.com",
                    "preferred_username", "preferred@example.com",
                    "name",               DISPLAY_NAME));

            when(props.isEnabled()).thenReturn(true);
            when(props.getIssuer()).thenReturn(null);
            when(props.getAudience()).thenReturn(null);
            when(jwtDecoder.decode(RAW_TOKEN)).thenReturn(jwt);

            User user = activeAdUser(1L);
            user.getGroups().add(new UserGroup()); // has groups → no default assignment
            when(userRepository.findByAdObjectId(AD_OBJECT_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(ldapGroupService.getGroupsForUser(USER_EMAIL)).thenReturn(List.of());
            when(jwtService.generateAccessToken(user)).thenReturn(ACCESS_TOKEN);
            when(jwtService.generateRefreshToken(user)).thenReturn(REFRESH_TOKEN);
            when(jwtService.extractExpiration(any())).thenReturn(new Date());
            when(userLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.adLogin(request());

            verify(ldapGroupService).getGroupsForUser(USER_EMAIL);
        }

        @Test
        @DisplayName("Falls back to 'upn' claim when 'email' is absent")
        void shouldFallBackToUpnWhenEmailAbsent() throws Exception {
            String upnEmail = "upn@corp.example.com";
            Jwt jwt = buildJwt(Map.of(
                    "oid",  AD_OBJECT_ID,
                    "upn",  upnEmail,
                    "name", DISPLAY_NAME));

            when(props.isEnabled()).thenReturn(true);
            when(props.getIssuer()).thenReturn(null);
            when(props.getAudience()).thenReturn(null);
            when(jwtDecoder.decode(RAW_TOKEN)).thenReturn(jwt);

            User user = activeAdUser(1L);
            user.getGroups().add(new UserGroup());
            when(userRepository.findByAdObjectId(AD_OBJECT_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(ldapGroupService.getGroupsForUser(upnEmail)).thenReturn(List.of());
            when(jwtService.generateAccessToken(user)).thenReturn(ACCESS_TOKEN);
            when(jwtService.generateRefreshToken(user)).thenReturn(REFRESH_TOKEN);
            when(jwtService.extractExpiration(any())).thenReturn(new Date());
            when(userLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.adLogin(request());

            verify(ldapGroupService).getGroupsForUser(upnEmail);
        }

        @Test
        @DisplayName("Falls back to 'preferred_username' when 'email' and 'upn' are both absent")
        void shouldFallBackToPreferredUsernameAsLastResort() throws Exception {
            String preferred = "preferred@example.com";
            Jwt jwt = buildJwt(Map.of(
                    "oid",                AD_OBJECT_ID,
                    "preferred_username", preferred,
                    "name",               DISPLAY_NAME));

            when(props.isEnabled()).thenReturn(true);
            when(props.getIssuer()).thenReturn(null);
            when(props.getAudience()).thenReturn(null);
            when(jwtDecoder.decode(RAW_TOKEN)).thenReturn(jwt);

            User user = activeAdUser(1L);
            user.getGroups().add(new UserGroup());
            when(userRepository.findByAdObjectId(AD_OBJECT_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(ldapGroupService.getGroupsForUser(preferred)).thenReturn(List.of());
            when(jwtService.generateAccessToken(user)).thenReturn(ACCESS_TOKEN);
            when(jwtService.generateRefreshToken(user)).thenReturn(REFRESH_TOKEN);
            when(jwtService.extractExpiration(any())).thenReturn(new Date());
            when(userLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.adLogin(request());

            verify(ldapGroupService).getGroupsForUser(preferred);
        }

        @Test
        @DisplayName("Uses 'oid' claim as adObjectId when present (Azure AD primary identifier)")
        void shouldUseOidClaimAsAdObjectId() throws Exception {
            String oid = "azure-oid-guid-5678";
            Jwt jwt = buildJwt(Map.of(
                    "oid",   oid,
                    "email", USER_EMAIL,
                    "name",  DISPLAY_NAME));

            when(props.isEnabled()).thenReturn(true);
            when(props.getIssuer()).thenReturn(null);
            when(props.getAudience()).thenReturn(null);
            when(jwtDecoder.decode(RAW_TOKEN)).thenReturn(jwt);

            User user = activeAdUser(1L);
            user.getGroups().add(new UserGroup());
            when(userRepository.findByAdObjectId(oid)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(ldapGroupService.getGroupsForUser(USER_EMAIL)).thenReturn(List.of());
            when(jwtService.generateAccessToken(user)).thenReturn(ACCESS_TOKEN);
            when(jwtService.generateRefreshToken(user)).thenReturn(REFRESH_TOKEN);
            when(jwtService.extractExpiration(any())).thenReturn(new Date());
            when(userLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.adLogin(request());

            verify(userRepository).findByAdObjectId(oid);
        }

        @Test
        @DisplayName("Falls back to 'sub' claim as adObjectId when 'oid' is absent (Keycloak / generic OIDC)")
        void shouldFallBackToSubWhenOidAbsent() throws Exception {
            String sub = "keycloak-sub-uuid";
            Jwt jwt = buildJwt(Map.of("email", USER_EMAIL, "name", DISPLAY_NAME));
            // 'oid' is absent — sub is set in buildJwt defaults as AD_OBJECT_ID
            // Override sub directly:
            Map<String, Object> claims = new HashMap<>(jwt.getClaims());
            claims.put("sub", sub);
            Jwt jwtWithSub = new Jwt(RAW_TOKEN, Instant.now(), Instant.now().plusSeconds(3600),
                    Map.of("alg", "RS256", "typ", "JWT"), claims);

            when(props.isEnabled()).thenReturn(true);
            when(props.getIssuer()).thenReturn(null);
            when(props.getAudience()).thenReturn(null);
            when(jwtDecoder.decode(RAW_TOKEN)).thenReturn(jwtWithSub);

            User user = activeAdUser(1L);
            user.setAdObjectId(sub);
            user.getGroups().add(new UserGroup());
            when(userRepository.findByAdObjectId(sub)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(ldapGroupService.getGroupsForUser(USER_EMAIL)).thenReturn(List.of());
            when(jwtService.generateAccessToken(user)).thenReturn(ACCESS_TOKEN);
            when(jwtService.generateRefreshToken(user)).thenReturn(REFRESH_TOKEN);
            when(jwtService.extractExpiration(any())).thenReturn(new Date());
            when(userLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.adLogin(request());

            verify(userRepository).findByAdObjectId(sub);
        }

        private AdLoginRequestDto request() {
            AdLoginRequestDto req = new AdLoginRequestDto();
            req.setIdToken(RAW_TOKEN);
            return req;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // User provisioning — findOrProvisionUser()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("User provisioning — findOrProvisionUser()")
    class UserProvisioning {

        @Test
        @DisplayName("Finds existing user by AD Object ID and syncs email + displayName")
        void shouldFindAndSyncExistingUserByOid() throws Exception {
            Jwt jwt = buildJwt(Map.of("oid", AD_OBJECT_ID, "email", "new-email@corp.com", "name", "New Name"));

            User existing = activeAdUser(5L);
            existing.setEmail("old-email@corp.com");
            existing.setName("Old Name");
            existing.getGroups().add(new UserGroup());

            when(props.isEnabled()).thenReturn(true);
            when(props.getIssuer()).thenReturn(null);
            when(props.getAudience()).thenReturn(null);
            when(jwtDecoder.decode(RAW_TOKEN)).thenReturn(jwt);
            when(userRepository.findByAdObjectId(AD_OBJECT_ID)).thenReturn(Optional.of(existing));
            when(userRepository.save(existing)).thenReturn(existing);
            when(ldapGroupService.getGroupsForUser("new-email@corp.com")).thenReturn(List.of());
            when(jwtService.generateAccessToken(existing)).thenReturn(ACCESS_TOKEN);
            when(jwtService.generateRefreshToken(existing)).thenReturn(REFRESH_TOKEN);
            when(jwtService.extractExpiration(any())).thenReturn(new Date());
            when(userLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.adLogin(request());

            assertThat(existing.getEmail()).isEqualTo("new-email@corp.com");
            assertThat(existing.getName()).isEqualTo("New Name");
        }

        @Test
        @DisplayName("Links pre-provisioned local user to AD identity when found by email")
        void shouldLinkLocalUserByEmailWhenOidNotFound() throws Exception {
            Jwt jwt = buildJwt(Map.of("oid", AD_OBJECT_ID, "email", USER_EMAIL, "name", DISPLAY_NAME));

            User localUser = activeAdUser(10L);
            localUser.setAuthProvider(AuthProvider.LOCAL);
            localUser.setAdObjectId(null);
            localUser.getGroups().add(new UserGroup());

            when(props.isEnabled()).thenReturn(true);
            when(props.getIssuer()).thenReturn(null);
            when(props.getAudience()).thenReturn(null);
            when(jwtDecoder.decode(RAW_TOKEN)).thenReturn(jwt);
            when(userRepository.findByAdObjectId(AD_OBJECT_ID)).thenReturn(Optional.empty());
            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(localUser));
            when(userRepository.save(localUser)).thenReturn(localUser);
            when(ldapGroupService.getGroupsForUser(USER_EMAIL)).thenReturn(List.of());
            when(jwtService.generateAccessToken(localUser)).thenReturn(ACCESS_TOKEN);
            when(jwtService.generateRefreshToken(localUser)).thenReturn(REFRESH_TOKEN);
            when(jwtService.extractExpiration(any())).thenReturn(new Date());
            when(userLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.adLogin(request());

            assertThat(localUser.getAdObjectId()).isEqualTo(AD_OBJECT_ID);
            assertThat(localUser.getAuthProvider()).isEqualTo(AuthProvider.AZURE_AD);
        }

        @Test
        @DisplayName("Provisions a brand-new AZURE_AD user on first-time login (no OID or email match)")
        void shouldProvisionNewUserOnFirstLogin() throws Exception {
            Jwt jwt = buildJwt(Map.of("oid", AD_OBJECT_ID, "email", USER_EMAIL, "name", DISPLAY_NAME));

            when(props.isEnabled()).thenReturn(true);
            when(props.getIssuer()).thenReturn(null);
            when(props.getAudience()).thenReturn(null);
            when(jwtDecoder.decode(RAW_TOKEN)).thenReturn(jwt);
            when(userRepository.findByAdObjectId(AD_OBJECT_ID)).thenReturn(Optional.empty());
            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

            ArgumentCaptor<User> savedCaptor = ArgumentCaptor.forClass(User.class);

            // First save() call = provisioning; subsequent calls = lastLoginAt update
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                if (u.getId() == null) u.setId(99L);
                // Ensure groups are initialized for syncGroups checks
                if (u.getGroups() == null) u.setGroups(new HashSet<>());
                if (u.getDirectRoles() == null) u.setDirectRoles(new HashSet<>());
                if (u.getAddresses() == null) u.setAddresses(new ArrayList<>());
                return u;
            });

            when(ldapGroupService.getGroupsForUser(USER_EMAIL)).thenReturn(List.of());
            // new user has no groups → service will call getDefaultGroup()
            when(groupMappingService.getDefaultGroup()).thenReturn(Optional.empty());
            when(jwtService.generateAccessToken(any())).thenReturn(ACCESS_TOKEN);
            when(jwtService.generateRefreshToken(any())).thenReturn(REFRESH_TOKEN);
            when(jwtService.extractExpiration(any())).thenReturn(new Date());
            when(userLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            LoginResponseDto response = service.adLogin(request());

            assertThat(response.getAccessToken()).isEqualTo(ACCESS_TOKEN);
            // Verify a new user entity was created with the correct attributes
            verify(userRepository, times(2)).save(savedCaptor.capture());
            User provisioned = savedCaptor.getAllValues().get(0);
            assertThat(provisioned.getEmail()).isEqualTo(USER_EMAIL);
            assertThat(provisioned.getName()).isEqualTo(DISPLAY_NAME);
            assertThat(provisioned.getAdObjectId()).isEqualTo(AD_OBJECT_ID);
            assertThat(provisioned.getAuthProvider()).isEqualTo(AuthProvider.AZURE_AD);
            assertThat(provisioned.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(provisioned.getPassword()).startsWith("$AD$");
        }

        @Test
        @DisplayName("Uses email as displayName when 'name' claim is absent from the ID token")
        void shouldUseEmailAsDisplayNameWhenNameClaimAbsent() throws Exception {
            Jwt jwt = buildJwt(Map.of("oid", AD_OBJECT_ID, "email", USER_EMAIL));
            // 'name' claim is absent

            User user = activeAdUser(1L);
            user.getGroups().add(new UserGroup());

            when(props.isEnabled()).thenReturn(true);
            when(props.getIssuer()).thenReturn(null);
            when(props.getAudience()).thenReturn(null);
            when(jwtDecoder.decode(RAW_TOKEN)).thenReturn(jwt);
            when(userRepository.findByAdObjectId(AD_OBJECT_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(ldapGroupService.getGroupsForUser(USER_EMAIL)).thenReturn(List.of());
            when(jwtService.generateAccessToken(user)).thenReturn(ACCESS_TOKEN);
            when(jwtService.generateRefreshToken(user)).thenReturn(REFRESH_TOKEN);
            when(jwtService.extractExpiration(any())).thenReturn(new Date());
            when(userLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.adLogin(request());

            // displayName falls back to email, so user.name is set to the email value
            assertThat(user.getName()).isEqualTo(USER_EMAIL);
        }

        private AdLoginRequestDto request() {
            AdLoginRequestDto req = new AdLoginRequestDto();
            req.setIdToken(RAW_TOKEN);
            return req;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Account status guard
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Account status guard (PCI-DSS Req 8.2.6)")
    class AccountStatusGuard {

        private void stubTokenAndOidLookup(User user) {
            Jwt jwt = buildJwt(Map.of("oid", AD_OBJECT_ID, "email", USER_EMAIL, "name", DISPLAY_NAME));
            when(props.isEnabled()).thenReturn(true);
            when(props.getIssuer()).thenReturn(null);
            when(props.getAudience()).thenReturn(null);
            when(jwtDecoder.decode(RAW_TOKEN)).thenReturn(jwt);
            when(userRepository.findByAdObjectId(AD_OBJECT_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
        }

        @Test
        @DisplayName("Throws AdAuthenticationException for INACTIVE users — generic message does not leak status")
        void shouldBlockInactiveUser() {
            User inactive = activeAdUser(1L);
            inactive.setStatus(UserStatus.INACTIVE);
            stubTokenAndOidLookup(inactive);

            assertThatThrownBy(() -> service.adLogin(request()))
                    .isInstanceOf(AdAuthenticationException.class)
                    .hasMessageContaining("access denied")
                    .hasMessageNotContaining("inactive");
        }

        @Test
        @DisplayName("Throws AdAuthenticationException for DELETED (soft-deleted) users — generic message does not leak status")
        void shouldBlockDeletedUser() {
            User deleted = activeAdUser(2L);
            deleted.setStatus(UserStatus.DELETED);
            stubTokenAndOidLookup(deleted);

            assertThatThrownBy(() -> service.adLogin(request()))
                    .isInstanceOf(AdAuthenticationException.class)
                    .hasMessageContaining("access denied")
                    .hasMessageNotContaining("deleted");
        }

        @Test
        @DisplayName("Throws AdAuthenticationException for SUSPENDED users — generic message does not leak status")
        void shouldBlockSuspendedUser() {
            User suspended = activeAdUser(3L);
            suspended.setStatus(UserStatus.SUSPENDED);
            stubTokenAndOidLookup(suspended);

            assertThatThrownBy(() -> service.adLogin(request()))
                    .isInstanceOf(AdAuthenticationException.class)
                    .hasMessageContaining("access denied")
                    .hasMessageNotContaining("suspended");
        }

        @Test
        @DisplayName("Allows NEW status users to complete login (account not yet fully onboarded)")
        void shouldAllowNewStatusUser() {
            User newUser = activeAdUser(4L);
            newUser.setStatus(UserStatus.NEW);
            newUser.getGroups().add(new UserGroup()); // has groups → no default assignment

            Jwt jwt = buildJwt(Map.of("oid", AD_OBJECT_ID, "email", USER_EMAIL, "name", DISPLAY_NAME));
            when(props.isEnabled()).thenReturn(true);
            when(props.getIssuer()).thenReturn(null);
            when(props.getAudience()).thenReturn(null);
            when(jwtDecoder.decode(RAW_TOKEN)).thenReturn(jwt);
            when(userRepository.findByAdObjectId(AD_OBJECT_ID)).thenReturn(Optional.of(newUser));
            when(userRepository.save(newUser)).thenReturn(newUser);
            when(ldapGroupService.getGroupsForUser(USER_EMAIL)).thenReturn(List.of());
            when(jwtService.generateAccessToken(newUser)).thenReturn(ACCESS_TOKEN);
            when(jwtService.generateRefreshToken(newUser)).thenReturn(REFRESH_TOKEN);
            when(jwtService.extractExpiration(any())).thenReturn(new Date());
            when(userLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Should NOT throw
            LoginResponseDto result = service.adLogin(request());
            assertThat(result.getAccessToken()).isEqualTo(ACCESS_TOKEN);
        }

        private AdLoginRequestDto request() {
            AdLoginRequestDto req = new AdLoginRequestDto();
            req.setIdToken(RAW_TOKEN);
            return req;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Group synchronisation — syncGroups()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Group synchronisation — syncGroups()")
    class GroupSync {

        private User user;

        @BeforeEach
        void baseSetup() {
            user = activeAdUser(1L);
            when(props.isEnabled()).thenReturn(true);
            when(props.getIssuer()).thenReturn(null);
            when(props.getAudience()).thenReturn(null);
            when(jwtDecoder.decode(RAW_TOKEN)).thenReturn(
                    buildJwt(Map.of("oid", AD_OBJECT_ID, "email", USER_EMAIL, "name", DISPLAY_NAME)));
            when(userRepository.findByAdObjectId(AD_OBJECT_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(jwtService.generateAccessToken(user)).thenReturn(ACCESS_TOKEN);
            when(jwtService.generateRefreshToken(user)).thenReturn(REFRESH_TOKEN);
            when(jwtService.extractExpiration(any())).thenReturn(new Date());
            when(userLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("Replaces existing groups with LDAP-resolved set when LDAP returns groups")
        void shouldReplaceGroupsWithLdapResolvedSet() {
            UserGroup oldGroup  = new UserGroup(); oldGroup.setId(10L); oldGroup.setName("OLD");
            UserGroup newGroup  = new UserGroup(); newGroup.setId(20L); newGroup.setName("NEW");
            user.getGroups().add(oldGroup);

            LdapGroup ldapGroup = new LdapGroup("ad-gid-1", "New AD Group");

            when(ldapGroupService.getGroupsForUser(USER_EMAIL)).thenReturn(List.of(ldapGroup));
            when(groupMappingService.resolveLocalGroups(List.of(ldapGroup))).thenReturn(Set.of(newGroup));

            service.adLogin(request());

            assertThat(user.getGroups())
                    .containsExactly(newGroup)
                    .doesNotContain(oldGroup);
        }

        @Test
        @DisplayName("Leaves groups unchanged when LDAP returns empty and user already has groups")
        void shouldLeaveGroupsUnchangedWhenLdapEmptyAndUserHasGroups() {
            UserGroup existing = new UserGroup(); existing.setId(5L); existing.setName("EXISTING");
            user.getGroups().add(existing);

            when(ldapGroupService.getGroupsForUser(USER_EMAIL)).thenReturn(List.of());

            service.adLogin(request());

            assertThat(user.getGroups()).containsExactly(existing);
            verify(groupMappingService, never()).resolveLocalGroups(any());
        }

        @Test
        @DisplayName("Assigns default group when LDAP returns empty and user has no groups (first AD login)")
        void shouldAssignDefaultGroupWhenLdapEmptyAndUserHasNoGroups() {
            UserGroup defaultGroup = new UserGroup(); defaultGroup.setId(1L); defaultGroup.setName("RETAIL_CUSTOMER");
            // user.getGroups() is empty at this point

            when(ldapGroupService.getGroupsForUser(USER_EMAIL)).thenReturn(List.of());
            when(groupMappingService.getDefaultGroup()).thenReturn(Optional.of(defaultGroup));

            service.adLogin(request());

            assertThat(user.getGroups()).containsExactly(defaultGroup);
        }

        @Test
        @DisplayName("Handles missing default group gracefully — user gets no group (no exception)")
        void shouldNotThrowWhenDefaultGroupDoesNotExist() {
            // user has no groups, LDAP is empty, default group not configured
            when(ldapGroupService.getGroupsForUser(USER_EMAIL)).thenReturn(List.of());
            when(groupMappingService.getDefaultGroup()).thenReturn(Optional.empty());

            // Must complete without exception
            LoginResponseDto result = service.adLogin(request());
            assertThat(result.getAccessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(user.getGroups()).isEmpty();
        }

        private AdLoginRequestDto request() {
            AdLoginRequestDto req = new AdLoginRequestDto();
            req.setIdToken(RAW_TOKEN);
            return req;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Happy-path token issuance and audit log
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Happy-path — token issuance and audit log")
    class HappyPath {

        @Test
        @DisplayName("Issues access and refresh tokens and persists two UserLog entries")
        void shouldIssueTokensAndPersistTwoUserLogEntries() throws Exception {
            User user = activeAdUser(1L);
            user.getGroups().add(new UserGroup());
            stubHappyPath(user);

            AdLoginRequestDto req = new AdLoginRequestDto();
            req.setIdToken(RAW_TOKEN);

            LoginResponseDto result = service.adLogin(req);

            assertThat(result.getAccessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(result.getRefreshToken()).isEqualTo(REFRESH_TOKEN);

            // Exactly two log entries: one ACCESS, one REFRESH
            ArgumentCaptor<com.org.auth.entity.UserLog> logCaptor =
                    ArgumentCaptor.forClass(com.org.auth.entity.UserLog.class);
            verify(userLogRepository, times(2)).save(logCaptor.capture());

            List<com.org.auth.entity.UserLog> logs = logCaptor.getAllValues();
            assertThat(logs).extracting(ul -> ul.getTokenType())
                    .containsExactlyInAnyOrder(TokenType.ACCESS, TokenType.REFRESH);
        }

        @Test
        @DisplayName("Updates lastLoginAt on the user entity before persisting")
        void shouldUpdateLastLoginAt() throws Exception {
            User user = activeAdUser(1L);
            user.getGroups().add(new UserGroup());
            user.setLastLoginAt(null);
            stubHappyPath(user);

            AdLoginRequestDto req = new AdLoginRequestDto();
            req.setIdToken(RAW_TOKEN);

            service.adLogin(req);

            assertThat(user.getLastLoginAt()).isNotNull();
        }

        @Test
        @DisplayName("Returns LoginResponseDto with UserDto containing email, status, and authProvider")
        void shouldReturnCorrectUserDtoInResponse() throws Exception {
            User user = activeAdUser(42L);
            user.getGroups().add(new UserGroup());
            stubHappyPath(user);

            AdLoginRequestDto req = new AdLoginRequestDto();
            req.setIdToken(RAW_TOKEN);

            LoginResponseDto result = service.adLogin(req);

            assertThat(result.getUser()).isNotNull();
            assertThat(result.getUser().getId()).isEqualTo(42L);
            assertThat(result.getUser().getEmail()).isEqualTo(USER_EMAIL);
            assertThat(result.getUser().getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(result.getUser().getAuthProvider()).isEqualTo(AuthProvider.AZURE_AD);
        }
    }
}
