package com.org.auth.filter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.org.auth.security.UserPrincipal;
import com.org.auth.service.JwtService;
import com.org.auth.service.TokenBlacklistService;
import com.org.auth.utils.TokenType;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Validates the JWT Bearer token on every request and populates the
 * {@code SecurityContext} with a {@link UserPrincipal} built purely from
 * claims.
 *
 * <p>
 * Flow:
 * <ol>
 * <li>No {@code Authorization: Bearer} header → pass through (public endpoints
 * handle themselves).</li>
 * <li>Token signature / expiry invalid → write {@code 401} JSON and stop
 * chain.</li>
 * <li>Token type is not {@code ACCESS} → write {@code 401} JSON (NIST 800-63B
 * §7.1).</li>
 * <li>Valid ACCESS token → build {@link UserPrincipal} and set in
 * {@code SecurityContext}.</li>
 * </ol>
 * </p>
 *
 * <p>
 * Spring Boot auto-registers every {@code @Component} filter as a servlet
 * filter.
 * The {@code FilterRegistrationBean} in {@code SecurityConfig} disables that
 * registration
 * so the filter runs only inside the Spring Security chain (no
 * double-execution).
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // No Bearer token — let the chain continue; public endpoints handle themselves
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            // Step 1 — validate signature and expiry
            if (!jwtService.isTokenValid(token)) {
                writeUnauthorized(response, "Invalid or expired token");
                return;
            }

            // Step 2 — reject REFRESH tokens used as API tokens (NIST 800-63B §7.1)
            if (!TokenType.ACCESS.name().equals(jwtService.extractTokenType(token))) {
                writeUnauthorized(response, "Refresh tokens cannot be used for API authorisation");
                return;
            }

            // Step 3 — reject blacklisted tokens (revoked on logout / refresh rotation)
            String jti = jwtService.extractJti(token);
            if (tokenBlacklistService.isBlacklisted(jti)) {
                writeUnauthorized(response, "Token has been revoked");
                return;
            }

            // Step 4 — reject tokens issued before a user-level invalidation event
            // (password change, account suspension, admin-forced logout)
            Long userId = jwtService.extractUserId(token);
            java.time.Instant issuedAt = jwtService.extractIssuedAt(token).toInstant();
            if (tokenBlacklistService.isUserTokensInvalidated(userId, issuedAt)) {
                writeUnauthorized(response, "Session has been invalidated. Please log in again.");
                return;
            }

            // Step 5 — skip if already authenticated in this request
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                chain.doFilter(request, response);
                return;
            }

            // Step 6 — extract claims and build principal
            String email = jwtService.extractUsername(token);
            String name = jwtService.extractName(token);
            List<String> permissions = jwtService.extractPermissions(token);
            List<String> groups = jwtService.extractGroups(token);

            var authorities = permissions.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            UserPrincipal principal = new UserPrincipal(userId, email, name, authorities, groups);

            var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);

            log.debug("JWT authenticated — email=[{}] permissions=[{}]", email, permissions.size());

        } catch (Exception e) {
            log.warn("JWT filter rejected token: {}", e.getMessage());
            writeUnauthorized(response, "Invalid or expired token");
            return;
        }

        chain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"status\":\"FAIL\",\"message\":\"" + message + "\"}");
    }
}
