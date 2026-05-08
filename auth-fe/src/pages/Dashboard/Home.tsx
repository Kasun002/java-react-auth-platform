import { useEffect, useMemo, useState } from "react";
import PageMeta from "../../components/common/PageMeta";
import {
  UserCircleIcon,
  GroupIcon,
  LockIcon,
  BoltIcon,
} from "../../icons";
import Badge from "../../components/ui/badge/Badge";
import { getDashboardStats } from "../../services/adminService";
import type { DashboardStatsDto } from "../../types/admin";

// ── KPI Stat Card ─────────────────────────────────────────────────────────────

interface StatCardProps {
  label: string;
  value: number | string;
  icon: React.ReactNode;
  iconBg: string;
  sub?: React.ReactNode;
}

function StatCard({ label, value, icon, iconBg, sub }: StatCardProps) {
  return (
    <div className="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/[0.03] md:p-6">
      <div className={`flex items-center justify-center w-12 h-12 rounded-xl ${iconBg}`}>
        {icon}
      </div>
      <div className="mt-5">
        <span className="text-sm text-gray-500 dark:text-gray-400">{label}</span>
        <h4 className="mt-2 font-bold text-gray-800 text-title-sm dark:text-white/90">
          {value}
        </h4>
        {sub && <div className="mt-1">{sub}</div>}
      </div>
    </div>
  );
}

// ── Helpers ───────────────────────────────────────────────────────────────────

function formatTime(iso: string) {
  return new Date(iso).toLocaleString(undefined, {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function getInitials(name: string) {
  return name.split(" ").map((n) => n[0]).slice(0, 2).join("").toUpperCase();
}

const GROUP_TYPE_COLOR: Record<string, string> = {
  CUSTOMER: "bg-brand-500",
  STAFF: "bg-success-500",
  OVERSIGHT: "bg-warning-500",
  ADMIN: "bg-error-500",
};

// ── Skeleton ──────────────────────────────────────────────────────────────────

function Skeleton({ className }: { className?: string }) {
  return (
    <div className={`animate-pulse rounded bg-gray-100 dark:bg-gray-800 ${className ?? ""}`} />
  );
}

// ── Component ─────────────────────────────────────────────────────────────────

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

  const maxGroupMembers = topGroups[0]?.memberCount ?? 1;

  const totalUsers = stats?.kpi.totalUsers ?? 0;

  const groupTypeBreakdown = useMemo(() => {
    const map: Record<string, number> = {};
    (stats?.groupDistribution ?? []).forEach((g) => {
      map[g.type] = (map[g.type] ?? 0) + 1;
    });
    return map;
  }, [stats]);

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

      {/* ── KPI Stats ── */}
      <div className="grid grid-cols-2 gap-4 md:gap-6 lg:grid-cols-4 mb-6">
        {loading ? (
          Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/[0.03] md:p-6">
              <Skeleton className="w-12 h-12 rounded-xl" />
              <div className="mt-5 space-y-2">
                <Skeleton className="h-4 w-24" />
                <Skeleton className="h-7 w-16" />
              </div>
            </div>
          ))
        ) : (
          <>
            <StatCard
              label="Total Users"
              value={stats!.kpi.totalUsers}
              iconBg="bg-brand-50 dark:bg-brand-500/15"
              icon={<UserCircleIcon className="text-brand-500 size-6" />}
              sub={
                <span className="text-xs text-gray-400 dark:text-gray-500">
                  {stats!.users.active} active · {stats!.users.newUsers} new
                </span>
              }
            />
            <StatCard
              label="Groups"
              value={stats!.kpi.totalGroups}
              iconBg="bg-purple-50 dark:bg-purple-500/15"
              icon={<GroupIcon className="text-purple-500 size-6" />}
              sub={
                <span className="text-xs text-gray-400 dark:text-gray-500">
                  {Object.keys(groupTypeBreakdown).length} types
                </span>
              }
            />
            <StatCard
              label="Roles"
              value={stats!.kpi.totalRoles}
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
              value={stats!.kpi.totalPermissions}
              iconBg="bg-blue-50 dark:bg-blue-500/15"
              icon={<BoltIcon className="text-blue-500 size-6" />}
              sub={
                <span className="text-xs text-gray-400 dark:text-gray-500">
                  {stats!.permissionsByCategory.length} categories
                </span>
              }
            />
          </>
        )}
      </div>

      <div className="grid grid-cols-12 gap-4 md:gap-6">

        {/* ── User Status Breakdown ── */}
        <div className="col-span-12 lg:col-span-4">
          <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] h-full">
            <div className="px-6 py-5 border-b border-gray-100 dark:border-gray-800">
              <h3 className="text-base font-medium text-gray-800 dark:text-white/90">
                User Status
              </h3>
              <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                Account status breakdown
              </p>
            </div>
            <div className="p-6 space-y-4">
              {loading ? (
                Array.from({ length: 3 }).map((_, i) => (
                  <div key={i} className="space-y-1.5">
                    <Skeleton className="h-4 w-full" />
                    <Skeleton className="h-2 w-full" />
                  </div>
                ))
              ) : (
                <>
                  {[
                    { label: "Active",   count: stats!.users.active,   color: "bg-success-500" },
                    { label: "Inactive", count: stats!.users.inactive, color: "bg-gray-400" },
                    { label: "New",      count: stats!.users.newUsers, color: "bg-brand-500" },
                    { label: "Deleted",  count: stats!.users.deleted,  color: "bg-error-500" },
                  ].map(({ label, count, color }) => {
                    const pct = totalUsers > 0 ? Math.round((count / totalUsers) * 100) : 0;
                    return (
                      <div key={label}>
                        <div className="flex items-center justify-between mb-1.5">
                          <div className="flex items-center gap-2">
                            <span className={`w-2.5 h-2.5 rounded-full ${color}`} />
                            <span className="text-sm text-gray-600 dark:text-gray-300">{label}</span>
                          </div>
                          <span className="text-sm font-medium text-gray-800 dark:text-white/90">{count}</span>
                        </div>
                        <div className="h-2 w-full rounded-full bg-gray-100 dark:bg-gray-800">
                          <div
                            className={`h-2 rounded-full ${color} transition-all duration-500`}
                            style={{ width: `${pct}%` }}
                          />
                        </div>
                      </div>
                    );
                  })}

                  <div className="pt-4 border-t border-gray-100 dark:border-gray-800">
                    <p className="text-xs font-semibold uppercase tracking-wide text-gray-400 dark:text-gray-500 mb-3">
                      Auth Provider
                    </p>
                    <div className="flex items-center justify-between py-1">
                      <span className="text-sm text-gray-600 dark:text-gray-300">Local</span>
                      <Badge color="light" size="sm">{stats!.users.localAuth}</Badge>
                    </div>
                    <div className="flex items-center justify-between py-1">
                      <span className="text-sm text-gray-600 dark:text-gray-300">Azure AD</span>
                      <Badge color="light" size="sm">{stats!.users.azureAdAuth}</Badge>
                    </div>
                  </div>
                </>
              )}
            </div>
          </div>
        </div>

        {/* ── Group Distribution ── */}
        <div className="col-span-12 lg:col-span-8">
          <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] h-full">
            <div className="px-6 py-5 border-b border-gray-100 dark:border-gray-800">
              <h3 className="text-base font-medium text-gray-800 dark:text-white/90">
                Group Member Distribution
              </h3>
              <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                Members per group — sorted by size
              </p>
            </div>
            <div className="p-6 space-y-3">
              {loading ? (
                Array.from({ length: 6 }).map((_, i) => (
                  <div key={i} className="flex items-center gap-3">
                    <Skeleton className="h-4 w-40 shrink-0" />
                    <Skeleton className="h-2 flex-1" />
                    <Skeleton className="h-4 w-6" />
                  </div>
                ))
              ) : (
                topGroups.map(({ id, name, type, memberCount }) => (
                  <div key={id} className="flex items-center gap-3">
                    <span
                      className="text-sm text-gray-600 dark:text-gray-300 shrink-0 truncate"
                      style={{ width: "180px" }}
                      title={name}
                    >
                      {name}
                    </span>
                    <div className="flex-1 h-2 rounded-full bg-gray-100 dark:bg-gray-800">
                      <div
                        className={`h-2 rounded-full transition-all duration-500 ${GROUP_TYPE_COLOR[type] ?? "bg-brand-500"}`}
                        style={{ width: `${(memberCount / maxGroupMembers) * 100}%` }}
                      />
                    </div>
                    <span className="text-sm font-medium text-gray-800 dark:text-white/90 w-6 text-right tabular-nums">
                      {memberCount}
                    </span>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>

        {/* ── Permissions by Category ── */}
        <div className="col-span-12 lg:col-span-5">
          <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
            <div className="px-6 py-5 border-b border-gray-100 dark:border-gray-800">
              <h3 className="text-base font-medium text-gray-800 dark:text-white/90">
                Permissions by Category
              </h3>
              <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                Atomic operation codes per resource domain
              </p>
            </div>
            <div className="p-4 sm:p-6 space-y-1">
              {loading ? (
                Array.from({ length: 6 }).map((_, i) => (
                  <div key={i} className="flex items-center justify-between py-2.5">
                    <Skeleton className="h-4 w-28" />
                    <Skeleton className="h-5 w-16 rounded-full" />
                  </div>
                ))
              ) : (
                stats!.permissionsByCategory.map(({ category, count }) => {
                  const maxCat = Math.max(...stats!.permissionsByCategory.map((c) => c.count), 1);
                  return (
                    <div
                      key={category}
                      className="flex items-center justify-between py-2.5 border-b border-gray-100 dark:border-gray-800 last:border-0"
                    >
                      <div>
                        <p className="text-sm font-medium text-gray-700 dark:text-gray-200">{category}</p>
                        <div className="mt-1 h-1.5 w-32 rounded-full bg-gray-100 dark:bg-gray-800">
                          <div
                            className="h-1.5 rounded-full bg-brand-400 transition-all duration-500"
                            style={{ width: `${(count / maxCat) * 100}%` }}
                          />
                        </div>
                      </div>
                      <span className="text-sm font-semibold text-gray-700 dark:text-gray-200 tabular-nums">
                        {count}
                      </span>
                    </div>
                  );
                })
              )}
            </div>
          </div>
        </div>

        {/* ── Recent Login Activity ── */}
        <div className="col-span-12 lg:col-span-7">
          <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
            <div className="px-6 py-5 border-b border-gray-100 dark:border-gray-800">
              <h3 className="text-base font-medium text-gray-800 dark:text-white/90">
                Recent Login Activity
              </h3>
              <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                10 most-recent token issuances (PCI-DSS Req 10.2)
              </p>
            </div>
            <div className="overflow-x-auto">
              {loading ? (
                <div className="p-6 space-y-3">
                  {Array.from({ length: 5 }).map((_, i) => (
                    <div key={i} className="flex items-center gap-3">
                      <Skeleton className="w-7 h-7 rounded-full shrink-0" />
                      <Skeleton className="h-4 w-32" />
                      <Skeleton className="h-4 flex-1" />
                      <Skeleton className="h-4 w-20" />
                    </div>
                  ))}
                </div>
              ) : (
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-gray-100 dark:border-gray-800">
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wide dark:text-gray-400">
                        User
                      </th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wide dark:text-gray-400 hidden md:table-cell">
                        IP Address
                      </th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wide dark:text-gray-400 hidden md:table-cell">
                        Time
                      </th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wide dark:text-gray-400">
                        Token
                      </th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
                    {(stats?.recentLogins ?? []).map((entry, idx) => (
                      <tr key={idx} className="hover:bg-gray-50 dark:hover:bg-white/[0.02] transition-colors">
                        <td className="px-6 py-3">
                          <div className="flex items-center gap-2.5">
                            <div className="flex items-center justify-center w-7 h-7 rounded-full bg-brand-50 dark:bg-brand-500/15 text-brand-600 dark:text-brand-400 text-xs font-semibold shrink-0">
                              {getInitials(entry.userName)}
                            </div>
                            <div className="min-w-0">
                              <p className="text-gray-700 dark:text-gray-300 font-medium truncate max-w-[120px]">
                                {entry.userName.split(" ")[0]}
                              </p>
                              <p className="text-xs text-gray-400 dark:text-gray-500 truncate max-w-[120px]">
                                {entry.email}
                              </p>
                            </div>
                          </div>
                        </td>
                        <td className="px-4 py-3 text-xs font-mono text-gray-500 dark:text-gray-400 hidden md:table-cell">
                          {entry.ipAddress ?? "—"}
                        </td>
                        <td className="px-4 py-3 text-gray-400 dark:text-gray-500 text-xs hidden md:table-cell whitespace-nowrap">
                          {formatTime(entry.issuedAt)}
                        </td>
                        <td className="px-4 py-3">
                          <Badge
                            color={entry.tokenType === "ACCESS" ? "success" : "light"}
                            size="sm"
                          >
                            {entry.tokenType}
                          </Badge>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        </div>

        {/* ── Group Type Distribution ── */}
        <div className="col-span-12">
          <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
            <div className="px-6 py-5 border-b border-gray-100 dark:border-gray-800">
              <h3 className="text-base font-medium text-gray-800 dark:text-white/90">
                Group Type Distribution
              </h3>
              <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                All {stats?.kpi.totalGroups ?? "—"} groups across {loading ? "—" : Object.keys(groupTypeBreakdown).length} types
              </p>
            </div>
            <div className="p-4 sm:p-6">
              {loading ? (
                <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                  {Array.from({ length: 4 }).map((_, i) => (
                    <Skeleton key={i} className="h-20 rounded-xl" />
                  ))}
                </div>
              ) : (
                <>
                  <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 mb-6">
                    {(["CUSTOMER", "STAFF", "OVERSIGHT", "ADMIN"] as const).map((type) => {
                      const count = groupTypeBreakdown[type] ?? 0;
                      return (
                        <div
                          key={type}
                          className="flex flex-col items-center justify-center gap-1 rounded-xl border border-gray-200 dark:border-gray-700 py-4"
                        >
                          <span className={`w-3 h-3 rounded-full ${GROUP_TYPE_COLOR[type]}`} />
                          <span className="text-lg font-bold text-gray-800 dark:text-white/90 tabular-nums">{count}</span>
                          <span className="text-xs font-medium text-gray-500 dark:text-gray-400">{type}</span>
                        </div>
                      );
                    })}
                  </div>
                  <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 xl:grid-cols-7">
                    {(stats?.groupDistribution ?? []).map((g) => (
                      <div
                        key={g.id}
                        className="flex items-start gap-2 px-3 py-2.5 rounded-xl border border-gray-100 dark:border-gray-800 hover:border-gray-300 dark:hover:border-gray-600 transition-colors"
                        title={g.type}
                      >
                        <span
                          className={`mt-1 w-2 h-2 rounded-full shrink-0 ${GROUP_TYPE_COLOR[g.type] ?? "bg-gray-400"}`}
                        />
                        <div className="min-w-0">
                          <p className="text-xs font-mono text-gray-600 dark:text-gray-400 truncate leading-snug">
                            {g.name}
                          </p>
                          <p className="text-xs text-gray-400 dark:text-gray-500 tabular-nums">
                            {g.memberCount} member{g.memberCount !== 1 ? "s" : ""}
                          </p>
                        </div>
                      </div>
                    ))}
                  </div>
                </>
              )}
            </div>
          </div>
        </div>

      </div>
    </>
  );
}
