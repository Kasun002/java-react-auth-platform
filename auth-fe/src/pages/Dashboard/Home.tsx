import { useEffect, useMemo, useState } from "react";
import PageMeta from "../../components/common/PageMeta";
import { getDashboardStats } from "../../services/adminService";
import type { DashboardStatsDto } from "../../types/admin";
import GroupDistributionCard from "./GroupDistributionCard";
import GroupTypeDistributionCard from "./GroupTypeDistributionCard";
import KpiStatsGrid from "./KpiStatsGrid";
import PermissionsByCategoryCard from "./PermissionsByCategoryCard";
import RecentLoginsCard from "./RecentLoginsCard";
import UserStatusCard from "./UserStatusCard";

export default function Home() {
  const [stats, setStats] = useState<DashboardStatsDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getDashboardStats()
      .then((res) => setStats(res.data.data))
      .catch(() => setError("Failed to load dashboard statistics."))
      .finally(() => setLoading(false));
  }, []);

  const topGroups = useMemo(
    () =>
      [...(stats?.groupDistribution ?? [])]
        .sort((a, b) => b.memberCount - a.memberCount)
        .slice(0, 8),
    [stats]
  );

  const groupTypeBreakdown = useMemo(() => {
    const map: Record<string, number> = {};
    (stats?.groupDistribution ?? []).forEach((g) => {
      map[g.type] = (map[g.type] ?? 0) + 1;
    });
    return map;
  }, [stats]);

  const maxGroupMembers = topGroups[0]?.memberCount ?? 1;
  const totalUsers = stats?.kpi.totalUsers ?? 0;

  if (error) {
    return (
      <div className="flex items-center justify-center py-24 text-sm text-error-600 dark:text-error-400">
        {error}
      </div>
    );
  }

  return (
    <>
      <PageMeta
        title="RBAC Dashboard | Auth Platform"
        description="Role-based access control overview — users, roles, groups, permissions, and audit activity"
      />

      {/* KPI Stats */}
      <div className="grid grid-cols-2 gap-4 md:gap-6 lg:grid-cols-4 mb-6">
        <KpiStatsGrid
          stats={stats}
          loading={loading}
          groupTypeCount={Object.keys(groupTypeBreakdown).length}
        />
      </div>

      <div className="grid grid-cols-12 gap-4 md:gap-6">
        <div className="col-span-12 lg:col-span-4">
          <UserStatusCard
            users={stats?.users}
            totalUsers={totalUsers}
            loading={loading}
          />
        </div>

        <div className="col-span-12 lg:col-span-8">
          <GroupDistributionCard
            topGroups={topGroups}
            maxGroupMembers={maxGroupMembers}
            loading={loading}
          />
        </div>

        <div className="col-span-12 lg:col-span-5">
          <PermissionsByCategoryCard
            permissionsByCategory={stats?.permissionsByCategory ?? []}
            loading={loading}
          />
        </div>

        <div className="col-span-12 lg:col-span-7">
          <RecentLoginsCard
            recentLogins={stats?.recentLogins ?? []}
            loading={loading}
          />
        </div>

        <div className="col-span-12">
          <GroupTypeDistributionCard
            groupDistribution={stats?.groupDistribution ?? []}
            groupTypeBreakdown={groupTypeBreakdown}
            totalGroups={stats?.kpi.totalGroups ?? 0}
            loading={loading}
          />
        </div>
      </div>
    </>
  );
}
