import { memo } from "react";
import SearchInput from "../../components/ui/SearchInput";

interface Props {
  query: string;
  onQueryChange: (q: string) => void;
  statusFilter: string;
  onStatusChange: (s: string) => void;
  fetching: boolean;
  initialLoad: boolean;
  totalElements: number;
}

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
      <SearchInput
        value={query}
        onChange={onQueryChange}
        placeholder="Search actor, action or details…"
        className="w-80"
      />


      {/* Status filter */}
      <div className="flex items-center gap-1.5">
        <span className="text-xs font-medium text-gray-500 dark:text-gray-400">
          Status:
        </span>
        <select
          value={statusFilter}
          onChange={(e) => onStatusChange(e.target.value)}
          className="h-10 rounded-lg border border-gray-200 bg-white px-3 text-sm text-gray-700 focus:border-brand-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-gray-300"
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
