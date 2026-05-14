import { memo } from "react";
import Skeleton from "../../components/ui/skeleton/Skeleton";
import type { DashboardStatsDto } from "../../types/admin";

interface Props {
  permissionsByCategory: DashboardStatsDto["permissionsByCategory"];
  loading: boolean;
}

function PermissionsByCategoryCard({ permissionsByCategory, loading }: Readonly<Props>) {
  // Compute once — not inside .map()
  const maxCat = Math.max(...permissionsByCategory.map((c) => c.count), 1);

  return (
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
          permissionsByCategory.map(({ category, count }) => (
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
          ))
        )}
      </div>
    </div>
  );
}

export default memo(PermissionsByCategoryCard);
