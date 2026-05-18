package com.org.auth.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.org.auth.security.UserPrincipal;
import com.org.auth.utils.AuditStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Facade that resolves the current actor and originating IP from the Spring
 * request context and delegates to {@link AuditLogService}.
 *
 * <p>Inject this into service implementations instead of calling
 * {@link AuditLogService#record} directly. Both actor and IP are resolved
 * automatically — callers only need to supply the action semantics.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditHelper {

    private final AuditLogService auditLogService;

    /**
     * Records an audit entry using the actor and IP resolved from the current
     * request's {@code SecurityContext}.
     *
     * <p>If the principal cannot be resolved (e.g. in async contexts) the call
     * is a no-op and a warning is logged — it must never break the main flow.</p>
     *
     * @param action     machine-readable action code, e.g. {@code ROLE_ASSIGNED}
     * @param resource   resource type, e.g. {@code ROLE}, {@code GROUP}
     * @param resourceId ID of the affected resource instance (may be null)
     * @param details    human-readable description of what changed
     * @param status     outcome of the action
     */
    public void record(String action, String resource, String resourceId,
                       String details, AuditStatus status) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
                log.warn("Audit skipped — no UserPrincipal in SecurityContext for action=[{}]", action);
                return;
            }

            String ipAddress = resolveIp();
            auditLogService.record(
                    principal.getId(), principal.getName(),
                    action, resource, resourceId,
                    details, ipAddress, status);
        } catch (Exception e) {
            // Audit failure must NEVER propagate to the caller
            log.error("Audit record failed for action=[{}]: {}", action, e.getMessage(), e);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String resolveIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;

            String forwarded = attrs.getRequest().getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                // X-Forwarded-For may contain a comma-separated list; first entry is the client
                return forwarded.split(",")[0].trim();
            }
            return attrs.getRequest().getRemoteAddr();
        } catch (Exception e) {
            log.warn("Could not resolve request IP for audit: {}", e.getMessage());
            return null;
        }
    }
}
