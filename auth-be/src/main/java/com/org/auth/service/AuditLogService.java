package com.org.auth.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.org.auth.dto.AuditLogDto;
import com.org.auth.utils.AuditStatus;

public interface AuditLogService {

    /**
     * Returns a paginated, filtered view of the audit log.
     *
     * @param status optional status filter (null = all)
     * @param q      optional free-text search (null / blank = no filter)
     * @param pageable pagination + sort
     */
    Page<AuditLogDto> getAuditLogs(String status, String q, Pageable pageable);

    /**
     * Records a single admin action to the audit log.
     * Prefer calling this via {@code AuditHelper.record()} which resolves the
     * actor and IP automatically from the Spring request context.
     *
     * @param actorId    ID of the user who performed the action
     * @param actorName  display name of that user (denormalised for permanence)
     * @param action     machine-readable action code, e.g. {@code ROLE_ASSIGNED}
     * @param resource   resource type, e.g. {@code USER}, {@code ROLE}
     * @param resourceId identifier of the affected resource (may be null)
     * @param details    human-readable description of what changed
     * @param ipAddress  originating IP (PCI-DSS Req 10.2.4; may be null)
     * @param status     outcome of the action
     */
    void record(Long actorId, String actorName, String action, String resource,
                String resourceId, String details, String ipAddress, AuditStatus status);
}
