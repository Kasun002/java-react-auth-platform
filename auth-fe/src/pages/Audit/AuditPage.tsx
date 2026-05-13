import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import PageMeta from "../../components/common/PageMeta";
import Pagination from "../../components/ui/pagination/Pagination";
import {
  Table,
  TableBody,
  TableCell,
  TableHeader,
  TableRow,
} from "../../components/ui/table";
import { DocsIcon } from "../../icons";
import { getAuditLogs } from "../../services/adminService";
import type { AuditLogDto, PageDto } from "../../types/admin";
import AuditFilters from "./AuditFilters";
import AuditSummaryChips from "./AuditSummaryChips";
import AuditTableRow from "./AuditTableRow";
import { PAGE_SIZE, TABLE_COLUMNS } from "./auditUtils";

export default function AuditPage() {
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const [data, setData] = useState<PageDto<AuditLogDto> | null>(null);
  const [initialLoad, setInitialLoad] = useState(true);
  const [fetching, setFetching] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const fetchLogs = useCallback(
    (status: string, q: string, pageNum: number) => {
      setFetching(true);
      setError(null);
      getAuditLogs({
        page: pageNum,
        size: PAGE_SIZE,
        status: status === "ALL" ? undefined : status,
        q: q.trim() || undefined,
      })
        .then((res) => {
          if (res.data.data) setData(res.data.data);
        })
        .catch(() => setError("Failed to load audit logs. Please try again."))
        .finally(() => {
          setFetching(false);
          setInitialLoad(false);
        });
    },
    []
  );

  // Immediate refetch on status/page change
  useEffect(() => {
    fetchLogs(statusFilter, query, page);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [statusFilter, page]);

  // Debounced refetch on query change
  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setPage(0);
      fetchLogs(statusFilter, query, 0);
    }, 350);
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query]);

  const entries = useMemo(() => data?.content ?? [], [data]);
  const totalPages = data?.totalPages ?? 0;

  const counts = useMemo(
    () => ({
      success: entries.filter((e) => e.status === "SUCCESS").length,
      warning: entries.filter((e) => e.status === "WARNING").length,
      failure: entries.filter((e) => e.status === "FAILURE").length,
    }),
    [entries]
  );

  const handleStatusChange = useCallback((s: string) => {
    setStatusFilter(s);
    setPage(0);
  }, []);

  function renderRows() {
    if (initialLoad) {
      return (
        <TableRow>
          <TableCell className="px-6 py-12 text-center text-sm text-gray-400">
            <div className="flex flex-col items-center gap-2">
              <div className="size-6 animate-spin rounded-full border-2 border-brand-500 border-t-transparent" />
              Loading audit log…
            </div>
          </TableCell>
        </TableRow>
      );
    }
    if (entries.length === 0) {
      return (
        <TableRow>
          <TableCell className="px-6 py-12 text-center text-sm text-gray-400">
            <div className="flex flex-col items-center gap-2">
              <DocsIcon className="size-8 text-gray-300 dark:text-gray-600" />
              No audit entries match the current filters
            </div>
          </TableCell>
        </TableRow>
      );
    }
    return entries.map((entry) => (
      <AuditTableRow key={entry.id} entry={entry} />
    ));
  }

  return (
    <>
      <PageMeta
        title="Audit Log | Auth Platform"
        description="Per PCI-DSS v4 Req 10.2 — every admin action is recorded"
      />

      {/* Page header */}
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-800 dark:text-white/90">
            Audit Log
          </h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Per PCI-DSS v4 Req 10.2 — every admin action is recorded
          </p>
        </div>
        <button className="inline-flex items-center gap-2 rounded-lg border border-gray-200 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-white/5 transition-colors">
          <DocsIcon className="size-4" />
          Export CSV
        </button>
      </div>

      <AuditSummaryChips
        successCount={counts.success}
        warningCount={counts.warning}
        failureCount={counts.failure}
      />

      <AuditFilters
        query={query}
        onQueryChange={setQuery}
        statusFilter={statusFilter}
        onStatusChange={handleStatusChange}
        fetching={fetching}
        initialLoad={initialLoad}
        totalElements={data?.totalElements ?? 0}
      />

      {error && (
        <div className="mb-4 rounded-lg border border-error-200 bg-error-50 px-4 py-3 text-sm text-error-700 dark:border-error-800 dark:bg-error-900/20 dark:text-error-400">
          {error}
        </div>
      )}

      <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] overflow-hidden">
        <div
          className={`overflow-x-auto transition-opacity duration-150 ${
            fetching && !initialLoad ? "opacity-50" : "opacity-100"
          }`}
        >
          <Table>
            <TableHeader>
              <TableRow className="border-b border-gray-100 dark:border-gray-800">
                {TABLE_COLUMNS.map(({ label, className }) => (
                  <TableCell
                    key={label}
                    isHeader
                    className={`px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500 dark:text-gray-400 ${className}`}
                  >
                    {label}
                  </TableCell>
                ))}
              </TableRow>
            </TableHeader>
            <TableBody className="divide-y divide-gray-100 dark:divide-gray-800">
              {renderRows()}
            </TableBody>
          </Table>
        </div>

        {!initialLoad && totalPages > 1 && (
          <Pagination
            page={page}
            totalPages={totalPages}
            onPrev={() => setPage((p) => Math.max(0, p - 1))}
            onNext={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
          />
        )}
      </div>
    </>
  );
}
