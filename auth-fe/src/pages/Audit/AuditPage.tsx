import { useCallback, useEffect, useRef, useState } from "react";
import PageMeta from "../../components/common/PageMeta";
import Badge from "../../components/ui/badge/Badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHeader,
  TableRow,
} from "../../components/ui/table";
import { DocsIcon, CheckCircleIcon, AlertIcon, ErrorIcon } from "../../icons";
import { getAuditLogs } from "../../services/adminService";
import type { AuditLogDto } from "../../types/admin";
import type { PageDto } from "../../types/admin";

// ── Helpers ───────────────────────────────────────────────────────────────────

function formatDate(iso: string) {
  return new Date(iso).toLocaleString(undefined, {
    month: "short",
    day: "numeric",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
}

function getInitials(name: string) {
  return name
    .split(" ")
    .map((n) => n[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();
}

const PAGE_SIZE = 50;

// ── Component ─────────────────────────────────────────────────────────────────

export default function AuditPage() {
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);

  const [data, setData] = useState<PageDto<AuditLogDto> | null>(null);
  // `initialLoad` drives the full-table spinner (first paint only).
  // `fetching` drives the subtle dimming on subsequent refetches.
  const [initialLoad, setInitialLoad] = useState(true);
  const [fetching, setFetching] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Debounce the free-text search so we don't fire on every keystroke
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const fetchLogs = useCallback(
    (status: string, q: string, pageNum: number) => {
      setFetching(true);
      setError(null);
      getAuditLogs({
        page: pageNum,
        size: PAGE_SIZE,
        status: status !== "ALL" ? status : undefined,
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

  // Re-fetch when status filter or page changes immediately
  useEffect(() => {
    fetchLogs(statusFilter, query, page);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [statusFilter, page]);

  // Debounce re-fetch when free-text query changes
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

  const entries = data?.content ?? [];
  const totalElements = data?.totalElements ?? 0;
  const totalPages = data?.totalPages ?? 0;

  // Summary counts from current page (server already filtered)
  const successCount = entries.filter((a) => a.status === "SUCCESS").length;
  const failureCount = entries.filter((a) => a.status === "FAILURE").length;
  const warningCount = entries.filter((a) => a.status === "WARNING").length;

  return (
    <>
      <PageMeta
        title="Audit Log | Auth Platform"
        description="Per PCI-DSS v4 Req 10.2 — every admin action is recorded"
      />

      {/* ── Page header ── */}
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

      {/* ── Summary chips ── */}
      <div className="mb-4 flex flex-wrap gap-2">
        <div className="flex items-center gap-2 rounded-lg border border-gray-200 dark:border-gray-700 px-3 py-2">
          <CheckCircleIcon className="size-4 text-success-500" />
          <span className="text-sm text-gray-600 dark:text-gray-400">Success</span>
          <span className="text-sm font-semibold text-gray-800 dark:text-white/90 tabular-nums">
            {successCount}
          </span>
        </div>
        <div className="flex items-center gap-2 rounded-lg border border-gray-200 dark:border-gray-700 px-3 py-2">
          <AlertIcon className="size-4 text-warning-500" />
          <span className="text-sm text-gray-600 dark:text-gray-400">Warning</span>
          <span className="text-sm font-semibold text-gray-800 dark:text-white/90 tabular-nums">
            {warningCount}
          </span>
        </div>
        <div className="flex items-center gap-2 rounded-lg border border-gray-200 dark:border-gray-700 px-3 py-2">
          <ErrorIcon className="size-4 text-error-500" />
          <span className="text-sm text-gray-600 dark:text-gray-400">Failure</span>
          <span className="text-sm font-semibold text-gray-800 dark:text-white/90 tabular-nums">
            {failureCount}
          </span>
        </div>
      </div>

      {/* ── Filters ── */}
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
            onChange={(e) => setQuery(e.target.value)}
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
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setPage(0);
            }}
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

      {/* ── Error banner ── */}
      {error && (
        <div className="mb-4 rounded-lg border border-error-200 bg-error-50 px-4 py-3 text-sm text-error-700 dark:border-error-800 dark:bg-error-900/20 dark:text-error-400">
          {error}
        </div>
      )}

      {/* ── Table ── */}
      <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] overflow-hidden">
        <div className={`overflow-x-auto transition-opacity duration-150 ${fetching && !initialLoad ? "opacity-50" : "opacity-100"}`}>
          <Table>
            <TableHeader>
              <TableRow className="border-b border-gray-100 dark:border-gray-800">
                {[
                  { label: "Actor", className: "" },
                  { label: "Action", className: "hidden md:table-cell" },
                  { label: "Details", className: "hidden lg:table-cell" },
                  { label: "IP", className: "w-32 hidden xl:table-cell" },
                  { label: "Time", className: "w-44 hidden md:table-cell" },
                  { label: "Status", className: "w-28" },
                ].map(({ label, className }) => (
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
              {initialLoad ? (
                <TableRow>
                  <TableCell className="px-6 py-12 text-center text-sm text-gray-400">
                    <div className="flex flex-col items-center gap-2">
                      <div className="size-6 animate-spin rounded-full border-2 border-brand-500 border-t-transparent" />
                      Loading audit log…
                    </div>
                  </TableCell>
                </TableRow>
              ) : entries.length === 0 ? (
                <TableRow>
                  <TableCell className="px-6 py-12 text-center text-sm text-gray-400">
                    <div className="flex flex-col items-center gap-2">
                      <DocsIcon className="size-8 text-gray-300 dark:text-gray-600" />
                      No audit entries match the current filters
                    </div>
                  </TableCell>
                </TableRow>
              ) : (
                entries.map((entry) => (
                  <TableRow
                    key={entry.id}
                    className="hover:bg-gray-50 dark:hover:bg-white/[0.02] transition-colors"
                  >
                    {/* Actor */}
                    <TableCell className="px-6 py-3">
                      <div className="flex items-center gap-2.5">
                        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-brand-50 dark:bg-brand-500/15 text-brand-600 dark:text-brand-400 text-xs font-semibold">
                          {getInitials(entry.actorName)}
                        </div>
                        <div className="min-w-0">
                          <p className="text-sm font-medium text-gray-800 dark:text-white/90 truncate">
                            {entry.actorName.split(" ")[0]}
                          </p>
                          <p className="text-xs text-gray-400 dark:text-gray-500 font-mono">
                            {entry.resource}
                          </p>
                        </div>
                      </div>
                    </TableCell>

                    {/* Action */}
                    <TableCell className="px-6 py-3 hidden md:table-cell">
                      <div className="flex items-center gap-1.5">
                        {entry.status === "SUCCESS" && (
                          <CheckCircleIcon className="size-3.5 text-success-500 shrink-0" />
                        )}
                        {entry.status === "WARNING" && (
                          <AlertIcon className="size-3.5 text-warning-500 shrink-0" />
                        )}
                        {entry.status === "FAILURE" && (
                          <ErrorIcon className="size-3.5 text-error-500 shrink-0" />
                        )}
                        <span className="text-xs font-mono text-gray-700 dark:text-gray-300 whitespace-nowrap">
                          {entry.action.replace(/_/g, " ")}
                        </span>
                      </div>
                    </TableCell>

                    {/* Details */}
                    <TableCell className="px-6 py-3 hidden lg:table-cell">
                      <p className="text-sm text-gray-500 dark:text-gray-400 truncate max-w-xs">
                        {entry.details}
                      </p>
                    </TableCell>

                    {/* IP */}
                    <TableCell className="px-6 py-3 text-xs font-mono text-gray-500 dark:text-gray-400 hidden xl:table-cell">
                      {entry.ipAddress ?? "—"}
                    </TableCell>

                    {/* Time */}
                    <TableCell className="px-6 py-3 text-xs font-mono text-gray-500 dark:text-gray-400 whitespace-nowrap hidden md:table-cell">
                      {formatDate(entry.createdAt)}
                    </TableCell>

                    {/* Status */}
                    <TableCell className="px-6 py-3">
                      {entry.status === "SUCCESS" && (
                        <Badge color="success" size="sm">Success</Badge>
                      )}
                      {entry.status === "WARNING" && (
                        <Badge color="warning" size="sm">Warning</Badge>
                      )}
                      {entry.status === "FAILURE" && (
                        <Badge color="error" size="sm">Failure</Badge>
                      )}
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </div>

        {/* ── Pagination ── */}
        {!initialLoad && totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-gray-100 px-6 py-3 dark:border-gray-800">
            <span className="text-xs text-gray-400 dark:text-gray-500 tabular-nums">
              Page {page + 1} of {totalPages}
            </span>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="rounded-lg border border-gray-200 px-3 py-1.5 text-xs font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed dark:border-gray-700 dark:text-gray-300 dark:hover:bg-white/5"
              >
                Previous
              </button>
              <button
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="rounded-lg border border-gray-200 px-3 py-1.5 text-xs font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed dark:border-gray-700 dark:text-gray-300 dark:hover:bg-white/5"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>
    </>
  );
}
