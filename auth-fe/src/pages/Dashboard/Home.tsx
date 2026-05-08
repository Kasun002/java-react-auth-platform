import { useMemo } from "react";
import PageMeta from "../../components/common/PageMeta";
import {
  UserCircleIcon,
  GroupIcon,
  LockIcon,
  BoltIcon,
  DocsIcon,
  CheckCircleIcon,
  AlertIcon,
  ErrorIcon,
} from "../../icons";
import Badge from "../../components/ui/badge/Badge";
import {
  USERS,
  ROLES,
  GROUPS,
  PERMISSIONS,
  AUDIT,
  getPermissionsByCategory,
  getGroupMemberCounts,
  getUsersByStatus,
} from "../../temp_data/rbacData";

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

// ── Audit status icon ─────────────────────────────────────────────────────────

function AuditStatusIcon({ status }: { status: "SUCCESS" | "FAILURE" | "WARNING" }) {
  if (status === "SUCCESS")
    return <CheckCircleIcon className="w-4 h-4 text-success-500" />;
  if (status === "WARNING")
    return <AlertIcon className="w-4 h-4 text-warning-500" />;
  return <ErrorIcon className="w-4 h-4 text-error-500" />;
}

function auditStatusBadge(status: "SUCCESS" | "FAILURE" | "WARNING") {
  if (status === "SUCCESS") return <Badge color="success" size="sm">Success</Badge>;
  if (status === "WARNING") return <Badge color="warning" size="sm">Warning</Badge>;
  return <Badge color="error" size="sm">Failure</Badge>;
}

function formatTime(iso: string) {
  return new Date(iso).toLocaleString(undefined, {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

// ── Component ─────────────────────────────────────────────────────────────────

export default function Home() {
  const usersByStatus = useMemo(() => getUsersByStatus(), []);
  const permsByCategory = useMemo(() => getPermissionsByCategory(), []);
  const groupMemberCounts = useMemo(() => getGroupMemberCounts(), []);

  const activeUsers = usersByStatus["ACTIVE"] ?? 0;
  const inactiveUsers = usersByStatus["INACTIVE"] ?? 0;
  const suspendedUsers = usersByStatus["SUSPENDED"] ?? 0;

  const maxGroupMembers = groupMemberCounts[0]?.count ?? 1;
  const topGroups = groupMemberCounts.slice(0, 8);

  const recentAudit = [...AUDIT]
    .sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())
    .slice(0, 8);

  const categoryEntries = Object.entries(permsByCategory);

  const riskColors: Record<string, string> = {
    LOW: "bg-success-500",
    MEDIUM: "bg-warning-500",
    HIGH: "bg-orange-500",
    CRITICAL: "bg-error-500",
  };

  return (
    <>
      <PageMeta
        title="RBAC Dashboard | Auth Platform"
        description="Role-based access control overview — users, roles, groups, permissions, and audit activity"
      />

      {/* ── KPI Stats ── */}
      <div className="grid grid-cols-2 gap-4 md:gap-6 lg:grid-cols-4 mb-6">
        <StatCard
          label="Total Users"
          value={USERS.length}
          iconBg="bg-brand-50 dark:bg-brand-500/15"
          icon={<UserCircleIcon className="text-brand-500 size-6" />}
          sub={
            <span className="text-xs text-gray-400 dark:text-gray-500">
              {activeUsers} active · {suspendedUsers} suspended
            </span>
          }
        />
        <StatCard
          label="Groups"
          value={GROUPS.length}
          iconBg="bg-purple-50 dark:bg-purple-500/15"
          icon={<GroupIcon className="text-purple-500 size-6" />}
          sub={
            <span className="text-xs text-gray-400 dark:text-gray-500">
              across {new Set(GROUPS.map(g => g.department)).size} departments
            </span>
          }
        />
        <StatCard
          label="Roles"
          value={ROLES.length}
          iconBg="bg-warning-50 dark:bg-warning-500/15"
          icon={<LockIcon className="text-warning-500 size-6" />}
          sub={
            <span className="text-xs text-gray-400 dark:text-gray-500">
              {ROLES.filter(r => r.isSystemRole).length} system · {ROLES.filter(r => !r.isSystemRole).length} custom
            </span>
          }
        />
        <StatCard
          label="Permissions"
          value={PERMISSIONS.length}
          iconBg="bg-blue-50 dark:bg-blue-500/15"
          icon={<BoltIcon className="text-blue-500 size-6" />}
          sub={
            <span className="text-xs text-gray-400 dark:text-gray-500">
              {categoryEntries.length} categories
            </span>
          }
        />
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
              {[
                { label: "Active", count: activeUsers, color: "bg-success-500", pct: Math.round((activeUsers / USERS.length) * 100) },
                { label: "Inactive", count: inactiveUsers, color: "bg-gray-400", pct: Math.round((inactiveUsers / USERS.length) * 100) },
                { label: "Suspended", count: suspendedUsers, color: "bg-error-500", pct: Math.round((suspendedUsers / USERS.length) * 100) },
              ].map(({ label, count, color, pct }) => (
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
              ))}

              <div className="pt-4 border-t border-gray-100 dark:border-gray-800">
                <p className="text-xs font-semibold uppercase tracking-wide text-gray-400 dark:text-gray-500 mb-3">
                  Auth Provider
                </p>
                {(["AZURE_AD", "LOCAL", "LDAP"] as const).map((provider) => {
                  const count = USERS.filter(u => u.authProvider === provider).length;
                  return (
                    <div key={provider} className="flex items-center justify-between py-1">
                      <span className="text-sm text-gray-600 dark:text-gray-300">
                        {provider === "AZURE_AD" ? "Azure AD" : provider === "LOCAL" ? "Local" : "LDAP"}
                      </span>
                      <Badge color="light" size="sm">{count}</Badge>
                    </div>
                  );
                })}
              </div>
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
              {topGroups.map(({ name, count }) => (
                <div key={name} className="flex items-center gap-3">
                  <span
                    className="text-sm text-gray-600 dark:text-gray-300 shrink-0"
                    style={{ width: "180px", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}
                    title={name}
                  >
                    {name}
                  </span>
                  <div className="flex-1 h-2 rounded-full bg-gray-100 dark:bg-gray-800">
                    <div
                      className="h-2 rounded-full bg-brand-500 transition-all duration-500"
                      style={{ width: `${(count / maxGroupMembers) * 100}%` }}
                    />
                  </div>
                  <span className="text-sm font-medium text-gray-800 dark:text-white/90 w-6 text-right">
                    {count}
                  </span>
                </div>
              ))}
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
                Risk level distribution per category
              </p>
            </div>
            <div className="p-4 sm:p-6 space-y-3">
              {categoryEntries.map(([category, perms]) => {
                const criticalCount = perms.filter(p => p.riskLevel === "CRITICAL").length;
                const highCount = perms.filter(p => p.riskLevel === "HIGH").length;
                return (
                  <div
                    key={category}
                    className="flex items-center justify-between py-2.5 border-b border-gray-100 dark:border-gray-800 last:border-0"
                  >
                    <div>
                      <p className="text-sm font-medium text-gray-700 dark:text-gray-200">{category}</p>
                      <p className="text-xs text-gray-400 dark:text-gray-500 mt-0.5">
                        {perms.length} permission{perms.length !== 1 ? "s" : ""}
                      </p>
                    </div>
                    <div className="flex items-center gap-1.5">
                      {criticalCount > 0 && (
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-error-50 text-error-600 dark:bg-error-500/15 dark:text-error-400">
                          {criticalCount} critical
                        </span>
                      )}
                      {highCount > 0 && (
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-warning-50 text-warning-600 dark:bg-warning-500/15 dark:text-orange-400">
                          {highCount} high
                        </span>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>

        {/* ── Risk Level Summary ── */}
        <div className="col-span-12 lg:col-span-7">
          <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
            <div className="px-6 py-5 border-b border-gray-100 dark:border-gray-800">
              <h3 className="text-base font-medium text-gray-800 dark:text-white/90">
                Recent Audit Activity
              </h3>
              <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                Latest security and access events
              </p>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-100 dark:border-gray-800">
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wide dark:text-gray-400">
                      User
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wide dark:text-gray-400">
                      Action
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wide dark:text-gray-400 hidden md:table-cell">
                      Time
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wide dark:text-gray-400">
                      Status
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
                  {recentAudit.map((entry) => (
                    <tr key={entry.id} className="hover:bg-gray-50 dark:hover:bg-white/[0.02] transition-colors">
                      <td className="px-6 py-3">
                        <div className="flex items-center gap-2.5">
                          <div className="flex items-center justify-center w-7 h-7 rounded-full bg-brand-50 dark:bg-brand-500/15 text-brand-600 dark:text-brand-400 text-xs font-semibold shrink-0">
                            {entry.userName.split(" ").map(n => n[0]).slice(0, 2).join("")}
                          </div>
                          <span className="text-gray-700 dark:text-gray-300 font-medium truncate max-w-[120px]">
                            {entry.userName.split(" ")[0]}
                          </span>
                        </div>
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-1.5">
                          <AuditStatusIcon status={entry.status} />
                          <span className="text-gray-600 dark:text-gray-400 text-xs font-mono">
                            {entry.action.replace(/_/g, " ")}
                          </span>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-gray-400 dark:text-gray-500 text-xs hidden md:table-cell whitespace-nowrap">
                        {formatTime(entry.timestamp)}
                      </td>
                      <td className="px-4 py-3">
                        {auditStatusBadge(entry.status)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>

        {/* ── Permission Risk Overview ── */}
        <div className="col-span-12">
          <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
            <div className="px-6 py-5 border-b border-gray-100 dark:border-gray-800">
              <h3 className="text-base font-medium text-gray-800 dark:text-white/90 flex items-center gap-2">
                <DocsIcon className="size-5 text-gray-500 dark:text-gray-400" />
                Permission Risk Overview
              </h3>
              <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                All {PERMISSIONS.length} permissions across {categoryEntries.length} categories
              </p>
            </div>
            <div className="p-4 sm:p-6">
              <div className="flex flex-wrap gap-3 mb-5">
                {(["CRITICAL", "HIGH", "MEDIUM", "LOW"] as const).map((level) => {
                  const count = PERMISSIONS.filter(p => p.riskLevel === level).length;
                  return (
                    <div
                      key={level}
                      className="flex items-center gap-2 px-3 py-1.5 rounded-lg border border-gray-200 dark:border-gray-700"
                    >
                      <span className={`w-2.5 h-2.5 rounded-full ${riskColors[level]}`} />
                      <span className="text-sm text-gray-600 dark:text-gray-300">{level}</span>
                      <span className="text-sm font-semibold text-gray-800 dark:text-white/90">{count}</span>
                    </div>
                  );
                })}
              </div>
              <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6">
                {PERMISSIONS.map((perm) => (
                  <div
                    key={perm.id}
                    className="flex items-start gap-2 px-3 py-2.5 rounded-xl border border-gray-100 dark:border-gray-800 hover:border-gray-300 dark:hover:border-gray-600 transition-colors group"
                    title={perm.description}
                  >
                    <span className={`mt-1 w-2 h-2 rounded-full shrink-0 ${riskColors[perm.riskLevel]}`} />
                    <span className="text-xs font-mono text-gray-600 dark:text-gray-400 group-hover:text-gray-800 dark:group-hover:text-gray-200 break-all leading-snug">
                      {perm.name}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>

      </div>
    </>
  );
}
