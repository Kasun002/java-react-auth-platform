package com.org.auth.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.org.auth.dto.AuditLogDto;
import com.org.auth.dto.PageDto;
import com.org.auth.dto.ResponseDto;
import com.org.auth.service.AuditLogService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Read-only audit log endpoint — PCI-DSS v4 Req 10.2.
 *
 * <p>Requires the {@code AUDIT_READ} permission. Supports server-side filtering
 * by status and free-text search across actor, action, resource, and details.</p>
 */
@Slf4j
@Tag(name = "Admin — Audit Log", description = "Read-only access to the PCI-DSS admin audit trail. Requires AUDIT_READ.")
@RestController
@RequestMapping("/admin/audit-logs")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @Operation(
            summary = "List audit log entries (paginated)",
            description = "Returns audit log entries newest-first. "
                    + "Use `status` to filter by outcome and `q` for a free-text search across actor, action, resource, and details.")
    @ApiResponse(responseCode = "200", description = "Audit log page returned")
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    @ApiResponse(responseCode = "403", description = "Insufficient authority — requires AUDIT_READ",
            content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_READ')")
    public ResponseEntity<ResponseDto<PageDto<AuditLogDto>>> getAuditLogs(
            @Parameter(description = "Filter by outcome: SUCCESS, WARNING, or FAILURE. Omit for all.")
            @RequestParam(required = false) String status,

            @Parameter(description = "Free-text search across actor name, action, resource, and details.")
            @RequestParam(required = false) String q,

            @ParameterObject
            @PageableDefault(page = 0, size = 50, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.info("Admin: audit-log query status=[{}] q=[{}] page=[{}]", status, q, pageable.getPageNumber());

        Page<AuditLogDto> page = auditLogService.getAuditLogs(status, q, pageable);
        PageDto<AuditLogDto> data = PageDto.from(page);

        ResponseDto<PageDto<AuditLogDto>> response = new ResponseDto<>();
        response.setStatus(ResponseDto.Status.SUCCESS);
        response.setData(data);
        response.setMessage(data.getTotalElements() + " audit log entry/entries found");
        return ResponseEntity.ok(response);
    }
}
