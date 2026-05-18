package com.org.auth.filter;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.org.auth.security.UserPrincipal;
import com.org.auth.service.JwtService;
import com.org.auth.service.TokenBlacklistService;
import com.org.auth.utils.TokenType;

import jakarta.servlet.FilterChain;

@DisplayName("JwtAuthenticationFilter")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class JwtAuthenticationFilterTest {

    @Mock private JwtService            jwtService;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private FilterChain           filterChain;

    @InjectMocks private JwtAuthenticationFilter filter;

    private MockHttpServletRequest  request;
    private MockHttpServletResponse response;

    private static final String VALID_TOKEN = "valid.access.token";

    @BeforeEach
    void setUp() {
        request  = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── No Authorization header ───────────────────────────────────────────────

    @Nested
    @DisplayName("No Authorization header")
    class NoAuthHeader {

        @Test
        @DisplayName("Should pass request through when no Authorization header is present")
        void shouldPassThroughWithNoHeader() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(response.getStatus()).isEqualTo(200); // untouched
        }

        @Test
        @DisplayName("Should pass through when Authorization header is not Bearer")
        void shouldPassThroughForNonBearerHeader() throws Exception {
            request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should not set SecurityContext authentication when no header present")
        void shouldNotSetAuthenticationWithNoHeader() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    // ── Invalid / expired token ───────────────────────────────────────────────

    @Nested
    @DisplayName("Invalid or expired token")
    class InvalidToken {

        @Test
        @DisplayName("Should return 401 when token fails validation")
        void shouldReturn401ForInvalidToken() throws Exception {
            request.addHeader("Authorization", "Bearer bad.token");
            when(jwtService.isTokenValid("bad.token")).thenReturn(false);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(401);
            verify(filterChain, never()).doFilter(request, response);
        }

        @Test
        @DisplayName("Should write FAIL JSON body on 401")
        void shouldWriteJsonBodyOn401() throws Exception {
            request.addHeader("Authorization", "Bearer bad.token");
            when(jwtService.isTokenValid("bad.token")).thenReturn(false);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getContentType()).contains("application/json");
            assertThat(response.getContentAsString()).contains("FAIL");
        }

        @Test
        @DisplayName("Should return 401 when token parsing throws an exception")
        void shouldReturn401WhenParsingThrows() throws Exception {
            request.addHeader("Authorization", "Bearer corrupt.token");
            when(jwtService.isTokenValid("corrupt.token"))
                    .thenThrow(new RuntimeException("parse error"));

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(401);
            verify(filterChain, never()).doFilter(request, response);
        }
    }

    // ── REFRESH token used for API call ──────────────────────────────────────

    @Nested
    @DisplayName("REFRESH token rejection")
    class RefreshTokenRejection {

        @Test
        @DisplayName("Should return 401 when a REFRESH token is presented — NIST 800-63B §7.1")
        void shouldRejectRefreshToken() throws Exception {
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
            when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
            when(jwtService.extractTokenType(VALID_TOKEN)).thenReturn(TokenType.REFRESH.name());

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(401);
            verify(filterChain, never()).doFilter(request, response);
        }
    }

    // ── Blacklisted token ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Blacklisted token")
    class BlacklistedToken {

        @Test
        @DisplayName("Should return 401 when token jti is on the blacklist — revoked token")
        void shouldReturn401ForBlacklistedToken() throws Exception {
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
            when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
            when(jwtService.extractTokenType(VALID_TOKEN)).thenReturn(TokenType.ACCESS.name());
            when(jwtService.extractJti(VALID_TOKEN)).thenReturn("revoked-jti");
            when(tokenBlacklistService.isBlacklisted("revoked-jti")).thenReturn(true);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(401);
            verify(filterChain, never()).doFilter(request, response);
        }

        @Test
        @DisplayName("Should write FAIL JSON body when token is blacklisted")
        void shouldWriteJsonBodyForBlacklistedToken() throws Exception {
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
            when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
            when(jwtService.extractTokenType(VALID_TOKEN)).thenReturn(TokenType.ACCESS.name());
            when(jwtService.extractJti(VALID_TOKEN)).thenReturn("revoked-jti");
            when(tokenBlacklistService.isBlacklisted("revoked-jti")).thenReturn(true);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getContentType()).contains("application/json");
            assertThat(response.getContentAsString()).contains("FAIL");
        }
    }

    // ── Valid ACCESS token ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Valid ACCESS token")
    class ValidAccessToken {

        @BeforeEach
        void stubValidToken() {
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
            when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
            when(jwtService.extractTokenType(VALID_TOKEN)).thenReturn(TokenType.ACCESS.name());
            when(jwtService.extractJti(VALID_TOKEN)).thenReturn("test-jti-uuid");
            when(tokenBlacklistService.isBlacklisted("test-jti-uuid")).thenReturn(false);
            // Step 4 — user-level session invalidation check
            when(jwtService.extractUserId(VALID_TOKEN)).thenReturn(42L);
            when(jwtService.extractIssuedAt(VALID_TOKEN)).thenReturn(new Date());
            when(tokenBlacklistService.isUserTokensInvalidated(eq(42L), any())).thenReturn(false);
            // Steps 1–4 always run; Step 6 (user extraction) is skipped when auth is pre-populated.
            // Mark as lenient so shouldNotOverwriteExistingAuthentication does not flag unused stubs.
            lenient().when(jwtService.extractUsername(VALID_TOKEN)).thenReturn("john@example.com");
            lenient().when(jwtService.extractPermissions(VALID_TOKEN))
                    .thenReturn(List.of("ACCOUNT_VIEW", "TRANSACTION_VIEW"));
            lenient().when(jwtService.extractGroups(VALID_TOKEN)).thenReturn(List.of("RETAIL_CUSTOMER"));
        }

        @Test
        @DisplayName("Should continue filter chain on valid ACCESS token")
        void shouldContinueChain() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should populate SecurityContext with UserPrincipal")
        void shouldSetSecurityContext() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getPrincipal()).isInstanceOf(UserPrincipal.class);
        }

        @Test
        @DisplayName("Principal should contain correct email")
        void principalShouldHaveEmail() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            UserPrincipal principal = (UserPrincipal)
                    SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            assertThat(principal.getUsername()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("Principal should carry permission authorities")
        void principalShouldHaveAuthorities() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            var authorities = SecurityContextHolder.getContext()
                    .getAuthentication().getAuthorities();
            assertThat(authorities)
                    .extracting(a -> a.getAuthority())
                    .containsExactlyInAnyOrder("ACCOUNT_VIEW", "TRANSACTION_VIEW");
        }

        @Test
        @DisplayName("Principal should carry group membership")
        void principalShouldHaveGroups() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            UserPrincipal principal = (UserPrincipal)
                    SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            assertThat(principal.getGroups()).containsExactly("RETAIL_CUSTOMER");
        }

        @Test
        @DisplayName("Should not overwrite an existing SecurityContext authentication")
        void shouldNotOverwriteExistingAuthentication() throws Exception {
            // Pre-populate context (e.g., another filter already authenticated)
            var existingAuth = new org.springframework.security.authentication
                    .UsernamePasswordAuthenticationToken("existing", null, List.of());
            SecurityContextHolder.getContext().setAuthentication(existingAuth);

            // Steps 1–4 still execute; Step 5 sees existing auth and returns early without Step 6
            filter.doFilterInternal(request, response, filterChain);

            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth.getPrincipal()).isEqualTo("existing");
            verify(filterChain).doFilter(request, response);
        }
    }

    // ── User-level session invalidation (Step 4) ──────────────────────────────

    @Nested
    @DisplayName("User-level session invalidation")
    class UserLevelInvalidation {

        @BeforeEach
        void stubThroughStep3() {
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
            when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
            when(jwtService.extractTokenType(VALID_TOKEN)).thenReturn(TokenType.ACCESS.name());
            when(jwtService.extractJti(VALID_TOKEN)).thenReturn("test-jti-uuid");
            when(tokenBlacklistService.isBlacklisted("test-jti-uuid")).thenReturn(false);
            when(jwtService.extractUserId(VALID_TOKEN)).thenReturn(42L);
            when(jwtService.extractIssuedAt(VALID_TOKEN))
                    .thenReturn(new Date(System.currentTimeMillis() - 300_000)); // issued 5 min ago
        }

        @Test
        @DisplayName("Should return 401 when token was issued before a user-level invalidation event — password change")
        void shouldReturn401WhenSessionInvalidated() throws Exception {
            when(tokenBlacklistService.isUserTokensInvalidated(eq(42L), any())).thenReturn(true);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(401);
            verify(filterChain, never()).doFilter(request, response);
        }

        @Test
        @DisplayName("Should write FAIL JSON body containing session-invalidated message")
        void shouldWriteSessionInvalidatedMessage() throws Exception {
            when(tokenBlacklistService.isUserTokensInvalidated(eq(42L), any())).thenReturn(true);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getContentType()).contains("application/json");
            assertThat(response.getContentAsString()).contains("Session has been invalidated");
        }
    }
}
