import { memo } from "react";
import Skeleton from "../../components/ui/skeleton/Skeleton";
import { BoltIcon, GroupIcon, LockIcon, UserCircleIcon } from "../../icons";
import type { DashboardStatsDto } from "../../types/admin";

interface StatCardProps {
  label: string;
  value: number | string;
  icon: React.ReactNode;
  iconBg: string;
  sub?: React.ReactNode;
}

function StatCard({
  label,
  value,
  icon,
  iconBg,
  sub,
}: Readonly<StatCardProps>) {
  return (
    <div className="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/[0.03] md:p-6">
      <div
        className={`flex items-center justify-center w-12 h-12 rounded-xl ${iconBg}`}
      >
        {icon}
      </div>
      <div className="mt-5">
        <span className="text-sm text-gray-500 dark:text-gray-400">
          {label}
        </span>
        <h4 className="mt-2 font-bold text-gray-800 text-title-sm dark:text-white/90">
          {value}
        </h4>
        {sub && <div className="mt-1">{sub}</div>}
      </div>
    </div>
  );
}

interface Props {
  stats: DashboardStatsDto | null;
  loading: boolean;
  groupTypeCount: number;
}

function KpiStatsGrid({ stats, loading, groupTypeCount }: Readonly<Props>) {
  if (loading) {
    return (
      <>
        {Array.from({ length: 4 }).map((_, i) => (
          <div
            key={`kpi-${i + 1}`}
            className="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/[0.03] md:p-6"
          >
            <Skeleton className="w-12 h-12 rounded-xl" />
            <div className="mt-5 space-y-2">
              <Skeleton className="h-4 w-24" />
              <Skeleton className="h-7 w-16" />
            </div>
          </div>
        ))}
      </>
    );
  }

  return (
    <>
      <StatCard
        label="Total Users"
        value={stats?.kpi.totalUsers ?? 0}
        iconBg="bg-brand-50 dark:bg-brand-500/15"
        icon={<UserCircleIcon className="text-brand-500 size-6" />}
        sub={
          <span className="text-xs text-gray-400 dark:text-gray-500">
            {stats?.users.active ?? 0} active · {stats?.users.newUsers ?? 0} new
          </span>
        }
      />
      <StatCard
        label="Groups"
        value={stats?.kpi.totalGroups ?? 0}
        iconBg="bg-purple-50 dark:bg-purple-500/15"
        icon={<GroupIcon className="text-purple-500 size-6" />}
        sub={
          <span className="text-xs text-gray-400 dark:text-gray-500">
            {groupTypeCount} types
          </span>
        }
      />
      <StatCard
        label="Roles"
        value={stats?.kpi.totalRoles ?? 0}
        iconBg="bg-warning-50 dark:bg-warning-500/15"
        icon={<LockIcon className="text-warning-500 size-6" />}
        sub={
          <span className="text-xs text-gray-400 dark:text-gray-500">
            across all groups
          </span>
        }
      />
      <StatCard
        label="Permissions"
        value={stats?.kpi.totalPermissions ?? 0}
        iconBg="bg-blue-50 dark:bg-blue-500/15"
        icon={<BoltIcon className="text-blue-500 size-6" />}
        sub={
          <span className="text-xs text-gray-400 dark:text-gray-500">
            {stats?.permissionsByCategory.length ?? 0} categories
          </span>
        }
      />
    </>
  );
}

export default memo(KpiStatsGrid);
