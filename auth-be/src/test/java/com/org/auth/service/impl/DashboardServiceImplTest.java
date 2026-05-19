package com.org.auth.service.impl;

import com.org.auth.dto.DashboardStatsDto;
import com.org.auth.dto.DashboardStatsDto.CategoryPermCount;
import com.org.auth.dto.DashboardStatsDto.GroupMemberCount;
import com.org.auth.dto.DashboardStatsDto.KpiStats;
import com.org.auth.dto.DashboardStatsDto.RecentLoginDto;
import com.org.auth.dto.DashboardStatsDto.UserStats;
import com.org.auth.entity.Permission;
import com.org.auth.entity.User;
import com.org.auth.entity.UserGroup;
import com.org.auth.entity.UserLog;
import com.org.auth.repository.PermissionRepository;
import com.org.auth.repository.RoleRepository;
import com.org.auth.repository.UserGroupRepository;
import com.org.auth.repository.UserLogRepository;
import com.org.auth.repository.UserRepository;
import com.org.auth.utils.AuthProvider;
import com.org.auth.utils.TokenType;
import com.org.auth.utils.UserStatus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DashboardServiceImpl}.
 *
 * <p>Each {@code @Nested} class isolates one private builder method.
 * Repositories not under test in a given nested class are stubbed with safe
 * empty/zero defaults via {@code stubOtherRepos()} helpers so that Mockito's
 * STRICT_STUBS mode stays satisfied without coupling unrelated assertions.
 *
 * <p>No Spring context is loaded — all dependencies are Mockito mocks.
 */
@DisplayName("DashboardServiceImpl")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class DashboardServiceImplTest {

    @Mock private UserRepository       userRepository;
    @Mock private UserGroupRepository  userGroupRepository;
    @Mock private RoleRepository       roleRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private UserLogRepository    userLogRepository;

    @InjectMocks private DashboardServiceImpl service;

    // ── Entity factory helpers ────────────────────────────────────────────────

    private UserGroup makeGroup(Long id, String name, String type) {
        UserGroup g = new UserGroup();
        g.setId(id);
        g.setName(name);
        g.setType(type);
        return g;
    }

    private User makeUser(Long id, Set<UserGroup> groups) {
        User u = new User();
        u.setId(id);
        u.setName("User-" + id);
        u.setEmail("user" + id + "@example.com");
        u.setPassword("$2a$10$hash");
        u.setStatus(UserStatus.ACTIVE);
        u.setGroups(groups);
        return u;
    }

    private Permission makePermission(Long id, String code, String category) {
        Permission p = new Permission();
        p.setId(id);
        p.setCode(code);
        p.setCategory(category);
        return p;
    }

    private UserLog makeUserLog(Long id, User user, String ip, String userAgent,
                                LocalDateTime issuedAt, TokenType tokenType) {
        UserLog ul = new UserLog();
        ul.setId(id);
        ul.setUser(user);
        ul.setIpAddress(ip);
        ul.setUserAgent(userAgent);
        ul.setIssuedAt(issuedAt);
        ul.setTokenType(tokenType);
        ul.setUserToken("dummy-token");
        ul.setExpiresAt(issuedAt.plusHours(1));
        return ul;
    }

    // ── Stub helpers for repos NOT under focus in a given nested class ────────

    /** Stubs KPI + UserStats repos with zeros (used when testing other sections). */
    private void stubKpiAndUserStatsZero() {
        when(userRepository.count()).thenReturn(0L);
        when(userGroupRepository.count()).thenReturn(0L);
        when(roleRepository.count()).thenReturn(0L);
        when(permissionRepository.count()).thenReturn(0L);
        when(userRepository.countByStatus(any(UserStatus.class))).thenReturn(0L);
        when(userRepository.countByAuthProvider(any(AuthProvider.class))).thenReturn(0L);
    }

    /** Stubs groupDistribution + permsByCategory + recentLogins with empty results. */
    private void stubCollectionsEmpty() {
        when(userRepository.findAllWithGroups()).thenReturn(List.of());
        when(userGroupRepository.findAll()).thenReturn(List.of());
        when(permissionRepository.findAll()).thenReturn(List.of());
        when(userLogRepository.findTop10WithUserOrderByIssuedAtDesc(any(Pageable.class)))
                .thenReturn(List.of());
    }

    /** Full empty stub — every repo call returns zero/empty. */
    private void stubAllEmpty() {
        stubKpiAndUserStatsZero();
        stubCollectionsEmpty();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // getStats() — top-level assembly
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getStats() — top-level assembly")
    class GetStats {

        @Test
        @DisplayName("Returns a non-null DTO with all five sections present")
        void shouldReturnFullyAssembledDto() {
            stubAllEmpty();

            DashboardStatsDto result = service.getStats();

            assertThat(result).isNotNull();
            assertThat(result.getKpi()).isNotNull();
            assertThat(result.getUsers()).isNotNull();
            assertThat(result.getGroupDistribution()).isNotNull();
            assertThat(result.getPermissionsByCategory()).isNotNull();
            assertThat(result.getRecentLogins()).isNotNull();
        }

        @Test
        @DisplayName("All sections contain empty/zero values when all repos are empty")
        void shouldReturnAllZeroWhenReposEmpty() {
            stubAllEmpty();

            DashboardStatsDto result = service.getStats();

            assertThat(result.getKpi().getTotalUsers()).isZero();
            assertThat(result.getKpi().getTotalGroups()).isZero();
            assertThat(result.getKpi().getTotalRoles()).isZero();
            assertThat(result.getKpi().getTotalPermissions()).isZero();
            assertThat(result.getGroupDistribution()).isEmpty();
            assertThat(result.getPermissionsByCategory()).isEmpty();
            assertThat(result.getRecentLogins()).isEmpty();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // buildKpi() — KPI entity counts
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("KPI statistics — buildKpi()")
    class KpiStatistics {

        @Test
        @DisplayName("Reads exact count from each repository and populates all four KPI fields")
        void shouldPopulateAllFourKpiFieldsFromRepositories() {
            when(userRepository.count()).thenReturn(200L);
            when(userGroupRepository.count()).thenReturn(8L);
            when(roleRepository.count()).thenReturn(15L);
            when(permissionRepository.count()).thenReturn(40L);

            when(userRepository.countByStatus(any(UserStatus.class))).thenReturn(0L);
            when(userRepository.countByAuthProvider(any(AuthProvider.class))).thenReturn(0L);
            stubCollectionsEmpty();

            KpiStats kpi = service.getStats().getKpi();

            assertThat(kpi.getTotalUsers()).isEqualTo(200L);
            assertThat(kpi.getTotalGroups()).isEqualTo(8L);
            assertThat(kpi.getTotalRoles()).isEqualTo(15L);
            assertThat(kpi.getTotalPermissions()).isEqualTo(40L);
        }

        @Test
        @DisplayName("All KPI fields are zero when every repo.count() returns 0")
        void shouldReturnZeroKpiWhenReposEmpty() {
            stubAllEmpty();

            KpiStats kpi = service.getStats().getKpi();

            assertThat(kpi.getTotalUsers()).isZero();
            assertThat(kpi.getTotalGroups()).isZero();
            assertThat(kpi.getTotalRoles()).isZero();
            assertThat(kpi.getTotalPermissions()).isZero();
        }

        @Test
        @DisplayName("KPI counts are independent — one large value does not affect others")
        void shouldReportIndependentCountsPerEntity() {
            when(userRepository.count()).thenReturn(1_000_000L);
            when(userGroupRepository.count()).thenReturn(1L);
            when(roleRepository.count()).thenReturn(1L);
            when(permissionRepository.count()).thenReturn(1L);

            when(userRepository.countByStatus(any(UserStatus.class))).thenReturn(0L);
            when(userRepository.countByAuthProvider(any(AuthProvider.class))).thenReturn(0L);
            stubCollectionsEmpty();

            KpiStats kpi = service.getStats().getKpi();

            assertThat(kpi.getTotalUsers()).isEqualTo(1_000_000L);
            assertThat(kpi.getTotalGroups()).isEqualTo(1L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // buildUserStats() — status and auth-provider breakdown
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("User status and auth-provider breakdown — buildUserStats()")
    class UserStatusBreakdown {

        @Test
        @DisplayName("Maps every UserStatus variant and both AuthProvider values to correct fields")
        void shouldMapAllStatusVariantsAndBothProviders() {
            when(userRepository.count()).thenReturn(0L);
            when(userGroupRepository.count()).thenReturn(0L);
            when(roleRepository.count()).thenReturn(0L);
            when(permissionRepository.count()).thenReturn(0L);

            when(userRepository.countByStatus(UserStatus.ACTIVE)).thenReturn(90L);
            when(userRepository.countByStatus(UserStatus.INACTIVE)).thenReturn(20L);
            when(userRepository.countByStatus(UserStatus.NEW)).thenReturn(15L);
            when(userRepository.countByStatus(UserStatus.DELETED)).thenReturn(10L);
            when(userRepository.countByStatus(UserStatus.SUSPENDED)).thenReturn(5L);
            when(userRepository.countByAuthProvider(AuthProvider.LOCAL)).thenReturn(100L);
            when(userRepository.countByAuthProvider(AuthProvider.AZURE_AD)).thenReturn(40L);

            stubCollectionsEmpty();

            UserStats users = service.getStats().getUsers();

            assertThat(users.getActive()).isEqualTo(90L);
            assertThat(users.getInactive()).isEqualTo(20L);
            assertThat(users.getNewUsers()).isEqualTo(15L);
            assertThat(users.getDeleted()).isEqualTo(10L);
            assertThat(users.getSuspended()).isEqualTo(5L);
            assertThat(users.getLocalAuth()).isEqualTo(100L);
            assertThat(users.getAzureAdAuth()).isEqualTo(40L);
        }

        @Test
        @DisplayName("All UserStats fields are zero when user table is empty")
        void shouldReturnAllZeroWhenNoUsers() {
            stubAllEmpty();

            UserStats users = service.getStats().getUsers();

            assertThat(users.getActive()).isZero();
            assertThat(users.getInactive()).isZero();
            assertThat(users.getNewUsers()).isZero();
            assertThat(users.getDeleted()).isZero();
            assertThat(users.getSuspended()).isZero();
            assertThat(users.getLocalAuth()).isZero();
            assertThat(users.getAzureAdAuth()).isZero();
        }

        @Test
        @DisplayName("SUSPENDED count is tracked independently from INACTIVE and DELETED")
        void shouldTrackSuspendedIndependently() {
            when(userRepository.count()).thenReturn(0L);
            when(userGroupRepository.count()).thenReturn(0L);
            when(roleRepository.count()).thenReturn(0L);
            when(permissionRepository.count()).thenReturn(0L);

            when(userRepository.countByStatus(UserStatus.ACTIVE)).thenReturn(0L);
            when(userRepository.countByStatus(UserStatus.INACTIVE)).thenReturn(0L);
            when(userRepository.countByStatus(UserStatus.NEW)).thenReturn(0L);
            when(userRepository.countByStatus(UserStatus.DELETED)).thenReturn(0L);
            when(userRepository.countByStatus(UserStatus.SUSPENDED)).thenReturn(7L);
            when(userRepository.countByAuthProvider(any(AuthProvider.class))).thenReturn(0L);

            stubCollectionsEmpty();

            UserStats users = service.getStats().getUsers();

            assertThat(users.getSuspended()).isEqualTo(7L);
            assertThat(users.getInactive()).isZero();
            assertThat(users.getDeleted()).isZero();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // buildGroupDistribution() — per-group member counts
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Group distribution — buildGroupDistribution()")
    class GroupDistribution {

        private void stubOther() {
            stubKpiAndUserStatsZero();
            when(permissionRepository.findAll()).thenReturn(List.of());
            when(userLogRepository.findTop10WithUserOrderByIssuedAtDesc(any(Pageable.class)))
                    .thenReturn(List.of());
        }

        @Test
        @DisplayName("Returns empty list when no groups exist")
        void shouldReturnEmptyListWhenNoGroups() {
            stubOther();
            when(userRepository.findAllWithGroups()).thenReturn(List.of());
            when(userGroupRepository.findAll()).thenReturn(List.of());

            assertThat(service.getStats().getGroupDistribution()).isEmpty();
        }

        @Test
        @DisplayName("A group with no members has memberCount = 0")
        void shouldReturnZeroMemberCountForGroupWithNoMembers() {
            UserGroup solo = makeGroup(1L, "SOLO_GROUP", "SYSTEM");

            stubOther();
            when(userRepository.findAllWithGroups()).thenReturn(List.of());
            when(userGroupRepository.findAll()).thenReturn(List.of(solo));

            List<GroupMemberCount> dist = service.getStats().getGroupDistribution();

            assertThat(dist).hasSize(1);
            assertThat(dist.get(0).getId()).isEqualTo(1L);
            assertThat(dist.get(0).getName()).isEqualTo("SOLO_GROUP");
            assertThat(dist.get(0).getType()).isEqualTo("SYSTEM");
            assertThat(dist.get(0).getMemberCount()).isZero();
        }

        @Test
        @DisplayName("Counts members per group correctly when users belong to different groups")
        void shouldCountMembersPerGroupCorrectly() {
            UserGroup adminGroup = makeGroup(1L, "ADMIN", "SYSTEM");
            UserGroup staffGroup = makeGroup(2L, "STAFF", "SYSTEM");

            User alice = makeUser(1L, new HashSet<>(Set.of(adminGroup, staffGroup)));
            User bob   = makeUser(2L, new HashSet<>(Set.of(adminGroup)));
            User carol = makeUser(3L, new HashSet<>()); // no group membership

            stubOther();
            when(userRepository.findAllWithGroups()).thenReturn(List.of(alice, bob, carol));
            when(userGroupRepository.findAll()).thenReturn(List.of(adminGroup, staffGroup));

            // Sorted alphabetically: ADMIN < STAFF
            List<GroupMemberCount> dist = service.getStats().getGroupDistribution();

            assertThat(dist).hasSize(2);
            assertThat(dist.get(0).getName()).isEqualTo("ADMIN");
            assertThat(dist.get(0).getMemberCount()).isEqualTo(2L);
            assertThat(dist.get(1).getName()).isEqualTo("STAFF");
            assertThat(dist.get(1).getMemberCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Results are sorted alphabetically by group name regardless of insertion order")
        void shouldSortGroupsByNameAlphabetically() {
            UserGroup zeta  = makeGroup(3L, "ZETA",  "TYPE");
            UserGroup alpha = makeGroup(1L, "ALPHA", "TYPE");
            UserGroup mu    = makeGroup(2L, "MU",    "TYPE");

            stubOther();
            when(userRepository.findAllWithGroups()).thenReturn(List.of());
            // Return in non-alphabetical order — service must sort
            when(userGroupRepository.findAll()).thenReturn(List.of(zeta, alpha, mu));

            List<GroupMemberCount> dist = service.getStats().getGroupDistribution();

            assertThat(dist).extracting(GroupMemberCount::getName)
                    .containsExactly("ALPHA", "MU", "ZETA");
        }

        @Test
        @DisplayName("User belonging to multiple groups increments each group's count independently")
        void shouldIncrementEachGroupIndependentlyForMultiGroupUser() {
            UserGroup g1 = makeGroup(1L, "GROUP_A", "T");
            UserGroup g2 = makeGroup(2L, "GROUP_B", "T");
            UserGroup g3 = makeGroup(3L, "GROUP_C", "T");

            User multiGroupUser = makeUser(1L, new HashSet<>(Set.of(g1, g2, g3)));

            stubOther();
            when(userRepository.findAllWithGroups()).thenReturn(List.of(multiGroupUser));
            when(userGroupRepository.findAll()).thenReturn(List.of(g1, g2, g3));

            List<GroupMemberCount> dist = service.getStats().getGroupDistribution();

            assertThat(dist).hasSize(3)
                    .allMatch(gmc -> gmc.getMemberCount() == 1L);
        }

        @Test
        @DisplayName("Groups not referenced by any user still appear with memberCount = 0")
        void shouldRetainGroupsWithNoUsersWithZeroCount() {
            UserGroup active = makeGroup(1L, "ACTIVE_GROUP", "TYPE");
            UserGroup empty  = makeGroup(2L, "EMPTY_GROUP",  "TYPE");

            User u = makeUser(1L, new HashSet<>(Set.of(active)));

            stubOther();
            when(userRepository.findAllWithGroups()).thenReturn(List.of(u));
            when(userGroupRepository.findAll()).thenReturn(List.of(active, empty));

            List<GroupMemberCount> dist = service.getStats().getGroupDistribution();

            Map<String, Long> byName = new HashMap<>();
            dist.forEach(gmc -> byName.put(gmc.getName(), gmc.getMemberCount()));

            assertThat(byName.get("ACTIVE_GROUP")).isEqualTo(1L);
            assertThat(byName.get("EMPTY_GROUP")).isZero();
        }

        @Test
        @DisplayName("All group metadata (id, name, type) is correctly propagated to the DTO")
        void shouldMapGroupMetadataCorrectly() {
            UserGroup g = makeGroup(99L, "COMPLIANCE", "REGULATORY");

            stubOther();
            when(userRepository.findAllWithGroups()).thenReturn(List.of());
            when(userGroupRepository.findAll()).thenReturn(List.of(g));

            GroupMemberCount gmc = service.getStats().getGroupDistribution().get(0);

            assertThat(gmc.getId()).isEqualTo(99L);
            assertThat(gmc.getName()).isEqualTo("COMPLIANCE");
            assertThat(gmc.getType()).isEqualTo("REGULATORY");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // buildPermsByCategory() — permissions grouped by category
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Permissions by category — buildPermsByCategory()")
    class PermsByCategory {

        private void stubOther() {
            stubKpiAndUserStatsZero();
            when(userRepository.findAllWithGroups()).thenReturn(List.of());
            when(userGroupRepository.findAll()).thenReturn(List.of());
            when(userLogRepository.findTop10WithUserOrderByIssuedAtDesc(any(Pageable.class)))
                    .thenReturn(List.of());
        }

        @Test
        @DisplayName("Returns empty list when permission table is empty")
        void shouldReturnEmptyListWhenNoPermissions() {
            stubOther();
            when(permissionRepository.findAll()).thenReturn(List.of());

            assertThat(service.getStats().getPermissionsByCategory()).isEmpty();
        }

        @Test
        @DisplayName("Groups permissions by category and calculates counts correctly")
        void shouldGroupAndCountByCategory() {
            stubOther();
            when(permissionRepository.findAll()).thenReturn(List.of(
                    makePermission(1L, "USER_VIEW",   "USER"),
                    makePermission(2L, "USER_CREATE", "USER"),
                    makePermission(3L, "ROLE_VIEW",   "ROLE"),
                    makePermission(4L, "AUDIT_VIEW",  "AUDIT")
            ));

            List<CategoryPermCount> result = service.getStats().getPermissionsByCategory();

            assertThat(result).hasSize(3);
            Map<String, Long> byCategory = new HashMap<>();
            result.forEach(c -> byCategory.put(c.getCategory(), c.getCount()));

            assertThat(byCategory.get("USER")).isEqualTo(2L);
            assertThat(byCategory.get("ROLE")).isEqualTo(1L);
            assertThat(byCategory.get("AUDIT")).isEqualTo(1L);
        }

        @Test
        @DisplayName("Results are sorted alphabetically by category name")
        void shouldSortResultsByCategoryAlphabetically() {
            stubOther();
            when(permissionRepository.findAll()).thenReturn(List.of(
                    makePermission(1L, "USER_VIEW",   "USER"),
                    makePermission(2L, "AUDIT_VIEW",  "AUDIT"),
                    makePermission(3L, "ROLE_VIEW",   "ROLE"),
                    makePermission(4L, "DASHBOARD_VIEW", "DASHBOARD")
            ));

            List<CategoryPermCount> result = service.getStats().getPermissionsByCategory();

            assertThat(result).extracting(CategoryPermCount::getCategory)
                    .containsExactly("AUDIT", "DASHBOARD", "ROLE", "USER");
        }

        @Test
        @DisplayName("Single category with multiple permissions returns one entry with total count")
        void shouldProduceSingleEntryForSingleCategory() {
            stubOther();
            when(permissionRepository.findAll()).thenReturn(List.of(
                    makePermission(1L, "PERM_A", "SYSTEM"),
                    makePermission(2L, "PERM_B", "SYSTEM"),
                    makePermission(3L, "PERM_C", "SYSTEM")
            ));

            List<CategoryPermCount> result = service.getStats().getPermissionsByCategory();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCategory()).isEqualTo("SYSTEM");
            assertThat(result.get(0).getCount()).isEqualTo(3L);
        }

        @Test
        @DisplayName("Each permission in its own category produces N entries each with count 1")
        void shouldProduceSeparateEntryForEachDistinctCategory() {
            stubOther();
            when(permissionRepository.findAll()).thenReturn(List.of(
                    makePermission(1L, "A_PERM", "CAT_A"),
                    makePermission(2L, "B_PERM", "CAT_B"),
                    makePermission(3L, "C_PERM", "CAT_C")
            ));

            List<CategoryPermCount> result = service.getStats().getPermissionsByCategory();

            assertThat(result).hasSize(3)
                    .allMatch(c -> c.getCount() == 1L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // buildRecentLogins() — recent login event mapping
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Recent logins — buildRecentLogins()")
    class RecentLogins {

        private void stubOther() {
            stubKpiAndUserStatsZero();
            when(userRepository.findAllWithGroups()).thenReturn(List.of());
            when(userGroupRepository.findAll()).thenReturn(List.of());
            when(permissionRepository.findAll()).thenReturn(List.of());
        }

        @Test
        @DisplayName("Returns empty list when user_log table is empty")
        void shouldReturnEmptyListWhenNoLogs() {
            stubOther();
            when(userLogRepository.findTop10WithUserOrderByIssuedAtDesc(any(Pageable.class)))
                    .thenReturn(List.of());

            assertThat(service.getStats().getRecentLogins()).isEmpty();
        }

        @Test
        @DisplayName("Maps all UserLog fields to RecentLoginDto correctly for ACCESS token")
        void shouldMapAllUserLogFieldsForAccessToken() {
            LocalDateTime issuedAt = LocalDateTime.of(2026, 1, 15, 9, 30);
            User alice = makeUser(42L, new HashSet<>());
            UserLog log = makeUserLog(1L, alice, "192.168.1.1", "Chrome/120", issuedAt, TokenType.ACCESS);

            stubOther();
            when(userLogRepository.findTop10WithUserOrderByIssuedAtDesc(any(Pageable.class)))
                    .thenReturn(List.of(log));

            List<RecentLoginDto> logins = service.getStats().getRecentLogins();

            assertThat(logins).hasSize(1);
            RecentLoginDto dto = logins.get(0);
            assertThat(dto.getUserId()).isEqualTo(42L);
            assertThat(dto.getUserName()).isEqualTo("User-42");
            assertThat(dto.getEmail()).isEqualTo("user42@example.com");
            assertThat(dto.getIpAddress()).isEqualTo("192.168.1.1");
            assertThat(dto.getUserAgent()).isEqualTo("Chrome/120");
            assertThat(dto.getIssuedAt()).isEqualTo(issuedAt);
            assertThat(dto.getTokenType()).isEqualTo("ACCESS");
        }

        @Test
        @DisplayName("Maps TokenType.REFRESH to string 'REFRESH' correctly")
        void shouldMapRefreshTokenTypeToString() {
            User u = makeUser(1L, new HashSet<>());
            UserLog log = makeUserLog(1L, u, "10.0.0.1", "curl/7.0",
                    LocalDateTime.now(), TokenType.REFRESH);

            stubOther();
            when(userLogRepository.findTop10WithUserOrderByIssuedAtDesc(any(Pageable.class)))
                    .thenReturn(List.of(log));

            assertThat(service.getStats().getRecentLogins().get(0).getTokenType())
                    .isEqualTo("REFRESH");
        }

        @Test
        @DisplayName("Null ipAddress and userAgent (nullable PCI-DSS fields) are passed through as null")
        void shouldHandleNullIpAddressAndUserAgent() {
            User u = makeUser(1L, new HashSet<>());
            UserLog log = makeUserLog(1L, u, null, null, LocalDateTime.now(), TokenType.ACCESS);

            stubOther();
            when(userLogRepository.findTop10WithUserOrderByIssuedAtDesc(any(Pageable.class)))
                    .thenReturn(List.of(log));

            RecentLoginDto dto = service.getStats().getRecentLogins().get(0);
            assertThat(dto.getIpAddress()).isNull();
            assertThat(dto.getUserAgent()).isNull();
        }

        @Test
        @DisplayName("Passes PageRequest(page=0, size=10) to repository — enforces the 10-login cap at query level")
        void shouldPassPageRequestWithSizeTenToRepository() {
            stubOther();
            when(userLogRepository.findTop10WithUserOrderByIssuedAtDesc(any(Pageable.class)))
                    .thenReturn(List.of());

            service.getStats();

            verify(userLogRepository).findTop10WithUserOrderByIssuedAtDesc(
                    argThat(p -> p.getPageSize() == 10 && p.getPageNumber() == 0));
        }

        @Test
        @DisplayName("Preserves the order returned by the repository (ORDER BY is the JPQL's responsibility)")
        void shouldPreserveRepositoryResultOrder() {
            LocalDateTime t1 = LocalDateTime.of(2026, 1, 10, 12, 0);
            LocalDateTime t2 = LocalDateTime.of(2026, 1, 9,  12, 0);
            LocalDateTime t3 = LocalDateTime.of(2026, 1, 8,  12, 0);

            User u1 = makeUser(1L, new HashSet<>());
            User u2 = makeUser(2L, new HashSet<>());
            User u3 = makeUser(3L, new HashSet<>());

            List<UserLog> logs = List.of(
                    makeUserLog(10L, u1, null, null, t1, TokenType.ACCESS),
                    makeUserLog(11L, u2, null, null, t2, TokenType.ACCESS),
                    makeUserLog(12L, u3, null, null, t3, TokenType.ACCESS)
            );

            stubOther();
            when(userLogRepository.findTop10WithUserOrderByIssuedAtDesc(any(Pageable.class)))
                    .thenReturn(logs);

            List<RecentLoginDto> result = service.getStats().getRecentLogins();

            assertThat(result).extracting(RecentLoginDto::getIssuedAt)
                    .containsExactly(t1, t2, t3);
        }

        @Test
        @DisplayName("Processes exactly 10 logs when repository returns the maximum page size")
        void shouldProcessTenLogsAtMaximumCapacity() {
            User u = makeUser(1L, new HashSet<>());
            LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);

            List<UserLog> tenLogs = new java.util.ArrayList<>();
            for (int i = 0; i < 10; i++) {
                tenLogs.add(makeUserLog((long) i, u, null, null,
                        base.minusHours(i), TokenType.ACCESS));
            }

            stubOther();
            when(userLogRepository.findTop10WithUserOrderByIssuedAtDesc(any(Pageable.class)))
                    .thenReturn(tenLogs);

            assertThat(service.getStats().getRecentLogins()).hasSize(10);
        }
    }
}
