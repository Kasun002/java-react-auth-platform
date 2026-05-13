import { memo } from "react";

interface Props {
  query: string;
  onQueryChange: (q: string) => void;
  statusFilter: string;
  onStatusChange: (s: string) => void;
  fetching: boolean;
  initialLoad: boolean;
  totalElements: number;
}

const inputCls =
  "h-10 rounded-lg border border-gray-200 bg-white px-3 text-sm text-gray-700 focus:border-brand-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-gray-300";

function AuditFilters({
  query,
  onQueryChange,
  statusFilter,
  onStatusChange,
  fetching,
  initialLoad,
  totalElements,
}: Readonly<Props>) {
  return (
    <div className="mb-4 flex flex-wrap items-center gap-3">
      {/* Search */}
      <div className="relative">
        <svg
          className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
        >
          <circle cx="11" cy="11" r="8" />
          <path d="m21 21-4.35-4.35" />
        </svg>
        <input
          value={query}
          onChange={(e) => onQueryChange(e.target.value)}
          placeholder="Search actor, action or details…"
          className="h-10 w-80 rounded-lg border border-gray-200 bg-white pl-9 pr-3 text-sm text-gray-800 placeholder-gray-400 focus:border-brand-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-white/90 dark:placeholder-gray-500"
        />
      </div>

      {/* Status filter */}
      <div className="flex items-center gap-1.5">
        <span className="text-xs font-medium text-gray-500 dark:text-gray-400">
          Status:
        </span>
        <select
          value={statusFilter}
          onChange={(e) => onStatusChange(e.target.value)}
          className={inputCls}
        >
          <option value="ALL">All</option>
          <option value="SUCCESS">Success</option>
          <option value="WARNING">Warning</option>
          <option value="FAILURE">Failure</option>
        </select>
      </div>

      <span className="ml-auto text-xs text-gray-400 dark:text-gray-500 tabular-nums">
        {fetching && !initialLoad ? "Updating…" : `${totalElements} total`}
      </span>
    </div>
  );
}

export default memo(AuditFilters);
