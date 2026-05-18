package com.org.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.org.auth.dto.DashboardStatsDto;
import com.org.auth.dto.ResponseDto;
import com.org.auth.service.DashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Dashboard API — provides a single aggregated statistics endpoint for the
 * admin frontend dashboard.
 *
 * <p>
 * All data is read-only. A single {@code GET /admin/dashboard/stats} call
 * returns KPI counts, user-status breakdowns, group distributions, permission
 * category totals, and recent login activity in one round-trip.
 * </p>
 *
 * <p>
 * Banking standards: PCI-DSS v4 Req 7.2 (least-privilege access review),
 * Req 10.2 (audit log visibility). Requires {@code DASHBOARD_VIEW} authority.
 * </p>
 */
@Slf4j
@Tag(name = "Admin — Dashboard", description = "Aggregate statistics for the admin dashboard. Requires DASHBOARD_VIEW access.")
@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Get dashboard statistics", description = "Returns a single aggregated snapshot: KPI counts (users/groups/roles/permissions), "
            + "user-status and auth-provider breakdowns, per-group member counts, "
            + "permissions by category, and the 10 most-recent login events. "
            + "All data is read-only. Per PCI-DSS v4 Req 7.2 — caller must hold DASHBOARD_VIEW authority.")
    @ApiResponse(responseCode = "200", description = "Dashboard statistics returned")
    @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    @ApiResponse(responseCode = "403", description = "Insufficient authority — DASHBOARD_VIEW required", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public ResponseEntity<ResponseDto<DashboardStatsDto>> getStats() {
        log.info("Admin: dashboard stats requested");
        DashboardStatsDto data = dashboardService.getStats();

        ResponseDto<DashboardStatsDto> response = new ResponseDto<>();
        response.setStatus(ResponseDto.Status.SUCCESS);
        response.setData(data);
        response.setMessage("Dashboard statistics retrieved successfully");
        return ResponseEntity.ok(response);
    }
}
