import { memo } from "react";
import Badge from "../../components/ui/badge/Badge";
import Skeleton from "../../components/ui/skeleton/Skeleton";
import type { DashboardStatsDto } from "../../types/admin";
import { formatTime, getInitials } from "./dashboardUtils";

interface Props {
  recentLogins: DashboardStatsDto["recentLogins"];
  loading: boolean;
}

function RecentLoginsCard({ recentLogins, loading }: Readonly<Props>) {
  return (
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
              <div key={`sk-${i + 1}`} className="flex items-center gap-3">
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
                {["User", "IP Address", "Time", "Token"].map((col, i) => (
                  <th
                    key={col}
                    className={`px-${
                      i === 0 ? "6" : "4"
                    } py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wide dark:text-gray-400${
                      i === 1 || i === 2 ? " hidden md:table-cell" : ""
                    }`}
                  >
                    {col}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
              {recentLogins.map((entry, idx) => (
                <tr
                  key={`idx-${idx + 1}`}
                  className="hover:bg-gray-50 dark:hover:bg-white/[0.02] transition-colors"
                >
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
  );
}

export default memo(RecentLoginsCard);
