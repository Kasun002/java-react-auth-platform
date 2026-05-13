import { memo } from "react";
import Skeleton from "../../components/ui/skeleton/Skeleton";
import type { DashboardStatsDto } from "../../types/admin";
import { GROUP_TYPE_COLOR } from "./dashboardUtils";

const KNOWN_TYPES = ["CUSTOMER", "STAFF", "OVERSIGHT", "ADMIN"] as const;

interface Props {
  groupDistribution: DashboardStatsDto["groupDistribution"];
  groupTypeBreakdown: Record<string, number>;
  totalGroups: number;
  loading: boolean;
}

function GroupTypeDistributionCard({
  groupDistribution,
  groupTypeBreakdown,
  totalGroups,
  loading,
}: Readonly<Props>) {
  return (
    <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
      <div className="px-6 py-5 border-b border-gray-100 dark:border-gray-800">
        <h3 className="text-base font-medium text-gray-800 dark:text-white/90">
          Group Type Distribution
        </h3>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          All {totalGroups || "—"} groups across{" "}
          {loading ? "—" : Object.keys(groupTypeBreakdown).length} types
        </p>
      </div>
      <div className="p-4 sm:p-6">
        {loading ? (
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            {Array.from({ length: 4 }).map((_, i) => (
              <Skeleton key={`grp-tp-sk-${i + 1}`} className="h-20 rounded-xl" />
            ))}
          </div>
        ) : (
          <>
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 mb-6">
              {KNOWN_TYPES.map((type) => (
                <div
                  key={type}
                  className="flex flex-col items-center justify-center gap-1 rounded-xl border border-gray-200 dark:border-gray-700 py-4"
                >
                  <span className={`w-3 h-3 rounded-full ${GROUP_TYPE_COLOR[type]}`} />
                  <span className="text-lg font-bold text-gray-800 dark:text-white/90 tabular-nums">
                    {groupTypeBreakdown[type] ?? 0}
                  </span>
                  <span className="text-xs font-medium text-gray-500 dark:text-gray-400">{type}</span>
                </div>
              ))}
            </div>
            <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 xl:grid-cols-7">
              {groupDistribution.map((g) => (
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
                      {g.memberCount} member{g.memberCount === 1 ? "" : "s"}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </>
        )}
      </div>
    </div>
  );
}

export default memo(GroupTypeDistributionCard);
