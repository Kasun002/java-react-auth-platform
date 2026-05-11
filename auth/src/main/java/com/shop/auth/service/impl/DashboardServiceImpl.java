package com.shop.auth.service.impl;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.auth.dto.DashboardStatsDto;
import com.shop.auth.dto.DashboardStatsDto.CategoryPermCount;
import com.shop.auth.dto.DashboardStatsDto.GroupMemberCount;
import com.shop.auth.dto.DashboardStatsDto.KpiStats;
import com.shop.auth.dto.DashboardStatsDto.RecentLoginDto;
import com.shop.auth.dto.DashboardStatsDto.UserStats;
import com.shop.auth.entity.Permission;
import com.shop.auth.entity.User;
import com.shop.auth.entity.UserGroup;
import com.shop.auth.entity.UserLog;
import com.shop.auth.repository.PermissionRepository;
import com.shop.auth.repository.RoleRepository;
import com.shop.auth.repository.UserGroupRepository;
import com.shop.auth.repository.UserLogRepository;
import com.shop.auth.repository.UserRepository;
import com.shop.auth.service.DashboardService;
import com.shop.auth.utils.AuthProvider;
import com.shop.auth.utils.UserStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Reads multiple aggregate counts in a single transaction and assembles
 * the {@link DashboardStatsDto} returned by the dashboard endpoint.
 *
 * <p>
 * All queries are read-only. Lazy collections are handled explicitly to
 * avoid N+1 issues — groups are fetched with a single JOIN FETCH query, and
 * recent login user data is resolved via a JOIN FETCH as well.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private static final int RECENT_LOGIN_LIMIT = 10;

    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserLogRepository userLogRepository;

    @Override
    public DashboardStatsDto getStats() {
        log.debug("Building dashboard statistics snapshot");

        DashboardStatsDto stats = new DashboardStatsDto();
        stats.setKpi(buildKpi());
        stats.setUsers(buildUserStats());
        stats.setGroupDistribution(buildGroupDistribution());
        stats.setPermissionsByCategory(buildPermsByCategory());
        stats.setRecentLogins(buildRecentLogins());

        log.debug("Dashboard stats assembled successfully");
        return stats;
    }

    // ── Private builders ──────────────────────────────────────────────────────

    private KpiStats buildKpi() {
        KpiStats kpi = new KpiStats();
        kpi.setTotalUsers(userRepository.count());
        kpi.setTotalGroups(userGroupRepository.count());
        kpi.setTotalRoles(roleRepository.count());
        kpi.setTotalPermissions(permissionRepository.count());
        return kpi;
    }

    private UserStats buildUserStats() {
        UserStats us = new UserStats();
        us.setActive(userRepository.countByStatus(UserStatus.ACTIVE));
        us.setInactive(userRepository.countByStatus(UserStatus.INACTIVE));
        us.setNewUsers(userRepository.countByStatus(UserStatus.NEW));
        us.setDeleted(userRepository.countByStatus(UserStatus.DELETED));
        us.setLocalAuth(userRepository.countByAuthProvider(AuthProvider.LOCAL));
        us.setAzureAdAuth(userRepository.countByAuthProvider(AuthProvider.AZURE_AD));
        return us;
    }

    private List<GroupMemberCount> buildGroupDistribution() {
        // Single JOIN FETCH to avoid N+1 — loads users with their groups in one query
        List<User> usersWithGroups = userRepository.findAllWithGroups();

        // Count members per group ID
        Map<Long, Long> memberCountById = usersWithGroups.stream()
                .flatMap(u -> u.getGroups().stream())
                .collect(Collectors.groupingBy(UserGroup::getId, Collectors.counting()));

        return userGroupRepository.findAll().stream()
                .sorted(Comparator.comparing(UserGroup::getName))
                .map(g -> {
                    GroupMemberCount gmc = new GroupMemberCount();
                    gmc.setId(g.getId());
                    gmc.setName(g.getName());
                    gmc.setType(g.getType());
                    gmc.setMemberCount(memberCountById.getOrDefault(g.getId(), 0L));
                    return gmc;
                })
                .collect(Collectors.toList());
    }

    private List<CategoryPermCount> buildPermsByCategory() {
        return permissionRepository.findAll().stream()
                .collect(Collectors.groupingBy(Permission::getCategory, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    CategoryPermCount cpc = new CategoryPermCount();
                    cpc.setCategory(e.getKey());
                    cpc.setCount(e.getValue());
                    return cpc;
                })
                .collect(Collectors.toList());
    }

    private List<RecentLoginDto> buildRecentLogins() {
        List<UserLog> logs = userLogRepository.findTop10WithUserOrderByIssuedAtDesc(
                PageRequest.of(0, RECENT_LOGIN_LIMIT));

        return logs.stream()
                .map(ul -> {
                    RecentLoginDto dto = new RecentLoginDto();
                    dto.setUserId(ul.getUser().getId());
                    dto.setUserName(ul.getUser().getName());
                    dto.setEmail(ul.getUser().getEmail());
                    dto.setIpAddress(ul.getIpAddress());
                    dto.setUserAgent(ul.getUserAgent());
                    dto.setIssuedAt(ul.getIssuedAt());
                    dto.setTokenType(ul.getTokenType().name());
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
