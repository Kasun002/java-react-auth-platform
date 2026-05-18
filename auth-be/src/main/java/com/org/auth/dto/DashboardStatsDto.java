package com.org.auth.dto;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Aggregate statistics DTO for the admin dashboard.
 *
 * <p>Collected in a single request to minimise round-trips from the frontend.
 * All counts are read-only snapshots; no mutations are performed here.</p>
 */
@Data
@Schema(description = "Aggregate dashboard statistics snapshot")
public class DashboardStatsDto {

    @Schema(description = "Top-level KPI counters")
    private KpiStats kpi;

    @Schema(description = "User breakdown by status and auth provider")
    private UserStats users;

    @Schema(description = "Per-group member counts, sorted by group name")
    private List<GroupMemberCount> groupDistribution;

    @Schema(description = "Permission counts grouped by category, sorted alphabetically")
    private List<CategoryPermCount> permissionsByCategory;

    @Schema(description = "10 most-recent login events across all users")
    private List<RecentLoginDto> recentLogins;

    // ── Nested DTOs ───────────────────────────────────────────────────────────

    @Data
    @Schema(description = "High-level entity counts")
    public static class KpiStats {
        private long totalUsers;
        private long totalGroups;
        private long totalRoles;
        private long totalPermissions;
    }

    @Data
    @Schema(description = "User breakdown by status and authentication provider")
    public static class UserStats {
        /** Users with status = ACTIVE */
        private long active;
        /** Users with status = INACTIVE */
        private long inactive;
        /** Users with status = NEW (registered but not yet verified) */
        private long newUsers;
        /** Users with status = DELETED (soft-deleted) */
        private long deleted;
        /** Users provisioned via local password registration */
        private long localAuth;
        /** Users provisioned via Azure AD SSO */
        private long azureAdAuth;
    }

    @Data
    @Schema(description = "Member count for a single group")
    public static class GroupMemberCount {
        private Long   id;
        private String name;
        private String type;
        private long   memberCount;
    }

    @Data
    @Schema(description = "Permission count for a single category")
    public static class CategoryPermCount {
        private String category;
        private long   count;
    }

    @Data
    @Schema(description = "Recent login event summary")
    public static class RecentLoginDto {
        private Long          userId;
        private String        userName;
        private String        email;
        private String        ipAddress;
        private String        userAgent;
        private LocalDateTime issuedAt;
        private String        tokenType;
    }
}
