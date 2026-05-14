import { memo } from "react";
import Badge from "../../components/ui/badge/Badge";
import Skeleton from "../../components/ui/skeleton/Skeleton";
import type { DashboardStatsDto } from "../../types/admin";

interface Props {
  users: DashboardStatsDto["users"] | undefined;
  totalUsers: number;
  loading: boolean;
}

const STATUS_ROWS = [
  { label: "Active",   key: "active"   as const, color: "bg-success-500" },
  { label: "Inactive", key: "inactive" as const, color: "bg-gray-400"    },
  { label: "New",      key: "newUsers" as const, color: "bg-brand-500"   },
  { label: "Deleted",  key: "deleted"  as const, color: "bg-error-500"   },
];

function UserStatusCard({ users, totalUsers, loading }: Readonly<Props>) {
  return (
    <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] h-full">
      <div className="px-6 py-5 border-b border-gray-100 dark:border-gray-800">
        <h3 className="text-base font-medium text-gray-800 dark:text-white/90">User Status</h3>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">Account status breakdown</p>
      </div>
      <div className="p-6 space-y-4">
        {loading ? (
          Array.from({ length: 3 }).map((_, i) => (
            <div key={`usr-sk-${i + 1}`} className="space-y-1.5">
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-2 w-full" />
            </div>
          ))
        ) : (
          <>
            {STATUS_ROWS.map(({ label, key, color }) => {
              const count = users?.[key] ?? 0;
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
                <Badge color="light" size="sm">{users?.localAuth ?? 0}</Badge>
              </div>
              <div className="flex items-center justify-between py-1">
                <span className="text-sm text-gray-600 dark:text-gray-300">Azure AD</span>
                <Badge color="light" size="sm">{users?.azureAdAuth ?? 0}</Badge>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

export default memo(UserStatusCard);
