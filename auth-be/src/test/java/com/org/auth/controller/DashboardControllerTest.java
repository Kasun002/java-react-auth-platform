package com.org.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.org.auth.dto.DashboardStatsDto;
import com.org.auth.dto.DashboardStatsDto.CategoryPermCount;
import com.org.auth.dto.DashboardStatsDto.GroupMemberCount;
import com.org.auth.dto.DashboardStatsDto.KpiStats;
import com.org.auth.dto.DashboardStatsDto.RecentLoginDto;
import com.org.auth.dto.DashboardStatsDto.UserStats;
import com.org.auth.exception.handler.GlobalExceptionHandler;
import com.org.auth.service.DashboardService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link DashboardController}.
 *
 * <p>Uses standalone MockMvc — Spring Security ({@code @PreAuthorize}) is NOT
 * enforced here; security tests belong in a separate slice test that loads the
 * full security filter chain. These tests focus on HTTP response shape, status
 * codes, message-converter behaviour, and correct delegation to the service.
 */
@DisplayName("DashboardController")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Register JavaTimeModule so LocalDateTime fields serialize as ISO-8601 strings
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new DashboardController(dashboardService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .build();
    }

    // ── Test fixture ──────────────────────────────────────────────────────────

    private DashboardStatsDto buildFullStats() {
        KpiStats kpi = new KpiStats();
        kpi.setTotalUsers(100L);
        kpi.setTotalGroups(5L);
        kpi.setTotalRoles(10L);
        kpi.setTotalPermissions(25L);

        UserStats users = new UserStats();
        users.setActive(80L);
        users.setInactive(10L);
        users.setNewUsers(5L);
        users.setDeleted(3L);
        users.setSuspended(2L);
        users.setLocalAuth(60L);
        users.setAzureAdAuth(40L);

        GroupMemberCount gmc = new GroupMemberCount();
        gmc.setId(1L);
        gmc.setName("ADMIN");
        gmc.setType("SYSTEM");
        gmc.setMemberCount(10L);

        CategoryPermCount cpc = new CategoryPermCount();
        cpc.setCategory("USER");
        cpc.setCount(5L);

        RecentLoginDto login = new RecentLoginDto();
        login.setUserId(42L);
        login.setUserName("Alice");
        login.setEmail("alice@example.com");
        login.setIpAddress("10.0.0.1");
        login.setUserAgent("Mozilla/5.0");
        login.setIssuedAt(LocalDateTime.of(2026, 1, 15, 9, 30));
        login.setTokenType("ACCESS");

        DashboardStatsDto stats = new DashboardStatsDto();
        stats.setKpi(kpi);
        stats.setUsers(users);
        stats.setGroupDistribution(List.of(gmc));
        stats.setPermissionsByCategory(List.of(cpc));
        stats.setRecentLogins(List.of(login));

        return stats;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /admin/dashboard/stats
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /admin/dashboard/stats")
    class GetStats {

        @Test
        @DisplayName("Returns 200 with status=SUCCESS and all KPI fields on happy path")
        void shouldReturn200WithKpiFields() throws Exception {
            when(dashboardService.getStats()).thenReturn(buildFullStats());

            mockMvc.perform(get("/admin/dashboard/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").value("Dashboard statistics retrieved successfully"))
                    .andExpect(jsonPath("$.data.kpi.totalUsers",       is(100)))
                    .andExpect(jsonPath("$.data.kpi.totalGroups",      is(5)))
                    .andExpect(jsonPath("$.data.kpi.totalRoles",       is(10)))
                    .andExpect(jsonPath("$.data.kpi.totalPermissions", is(25)));
        }

        @Test
        @DisplayName("Returns 200 with all user-status and auth-provider breakdowns")
        void shouldReturn200WithUserStatsBreakdown() throws Exception {
            when(dashboardService.getStats()).thenReturn(buildFullStats());

            mockMvc.perform(get("/admin/dashboard/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.users.active",      is(80)))
                    .andExpect(jsonPath("$.data.users.inactive",    is(10)))
                    .andExpect(jsonPath("$.data.users.newUsers",    is(5)))
                    .andExpect(jsonPath("$.data.users.deleted",     is(3)))
                    .andExpect(jsonPath("$.data.users.suspended",   is(2)))
                    .andExpect(jsonPath("$.data.users.localAuth",   is(60)))
                    .andExpect(jsonPath("$.data.users.azureAdAuth", is(40)));
        }

        @Test
        @DisplayName("Returns 200 with group distribution and permission category sections")
        void shouldReturn200WithGroupAndPermissionSections() throws Exception {
            when(dashboardService.getStats()).thenReturn(buildFullStats());

            mockMvc.perform(get("/admin/dashboard/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.groupDistribution",            hasSize(1)))
                    .andExpect(jsonPath("$.data.groupDistribution[0].name",    is("ADMIN")))
                    .andExpect(jsonPath("$.data.groupDistribution[0].type",    is("SYSTEM")))
                    .andExpect(jsonPath("$.data.groupDistribution[0].memberCount", is(10)))
                    .andExpect(jsonPath("$.data.permissionsByCategory",        hasSize(1)))
                    .andExpect(jsonPath("$.data.permissionsByCategory[0].category", is("USER")))
                    .andExpect(jsonPath("$.data.permissionsByCategory[0].count",    is(5)));
        }

        @Test
        @DisplayName("Returns 200 with recent login section containing all mapped fields")
        void shouldReturn200WithRecentLoginSection() throws Exception {
            when(dashboardService.getStats()).thenReturn(buildFullStats());

            mockMvc.perform(get("/admin/dashboard/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.recentLogins",                  hasSize(1)))
                    .andExpect(jsonPath("$.data.recentLogins[0].userId",        is(42)))
                    .andExpect(jsonPath("$.data.recentLogins[0].userName",      is("Alice")))
                    .andExpect(jsonPath("$.data.recentLogins[0].email",         is("alice@example.com")))
                    .andExpect(jsonPath("$.data.recentLogins[0].ipAddress",     is("10.0.0.1")))
                    .andExpect(jsonPath("$.data.recentLogins[0].tokenType",     is("ACCESS")));
        }

        @Test
        @DisplayName("Returns 200 with empty collection sections when system has no data")
        void shouldReturn200WithEmptyCollectionsWhenNoData() throws Exception {
            DashboardStatsDto empty = new DashboardStatsDto();
            empty.setKpi(new KpiStats());
            empty.setUsers(new UserStats());
            empty.setGroupDistribution(List.of());
            empty.setPermissionsByCategory(List.of());
            empty.setRecentLogins(List.of());

            when(dashboardService.getStats()).thenReturn(empty);

            mockMvc.perform(get("/admin/dashboard/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.groupDistribution",      hasSize(0)))
                    .andExpect(jsonPath("$.data.permissionsByCategory",  hasSize(0)))
                    .andExpect(jsonPath("$.data.recentLogins",           hasSize(0)));
        }

        @Test
        @DisplayName("Delegates to DashboardService exactly once per request")
        void shouldDelegateToServiceExactlyOnce() throws Exception {
            when(dashboardService.getStats()).thenReturn(buildFullStats());

            mockMvc.perform(get("/admin/dashboard/stats"))
                    .andExpect(status().isOk());

            verify(dashboardService, times(1)).getStats();
        }

        @Test
        @DisplayName("Returns 500 with FAIL status when service throws an unexpected RuntimeException")
        void shouldReturn500WhenServiceThrowsUnexpectedException() throws Exception {
            when(dashboardService.getStats()).thenThrow(new RuntimeException("Unexpected DB error"));

            mockMvc.perform(get("/admin/dashboard/stats"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value("FAIL"))
                    .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
        }
    }
}
