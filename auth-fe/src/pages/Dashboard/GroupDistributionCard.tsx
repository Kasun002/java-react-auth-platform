import { memo } from "react";
import Skeleton from "../../components/ui/skeleton/Skeleton";
import type { DashboardStatsDto } from "../../types/admin";
import { GROUP_TYPE_COLOR } from "./dashboardUtils";

interface Props {
  topGroups: DashboardStatsDto["groupDistribution"];
  maxGroupMembers: number;
  loading: boolean;
}

function GroupDistributionCard({ topGroups, maxGroupMembers, loading }: Readonly<Props>) {
  return (
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
            <div key={`grp-sk-${i + 1}`} className="flex items-center gap-3">
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
  );
}

export default memo(GroupDistributionCard);
