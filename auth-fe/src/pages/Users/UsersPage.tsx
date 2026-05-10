import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router";
import PageMeta from "../../components/common/PageMeta";
import Badge from "../../components/ui/badge/Badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHeader,
  TableRow,
} from "../../components/ui/table";
import { UserCircleIcon, GroupIcon } from "../../icons";
import { getUsers, listGroups } from "../../services/adminService";
import type { UserGroupDto } from "../../types/admin";
import type { UserDto } from "../../types/auth";

// ── Helpers ───────────────────────────────────────────────────────────────────

type StatusColor = "success" | "error" | "warning" | "light";

const STATUS_COLOR: Record<string, StatusColor> = {
  ACTIVE: "success",
  INACTIVE: "light",
  SUSPENDED: "error",
  NEW: "warning",
};

const AUTH_PROVIDER_LABEL: Record<string, string> = {
  LOCAL: "Local",
  AZURE_AD: "Azure AD",
};

function getInitials(name: string) {
  return name
    .split(" ")
    .map((n) => n[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();
}

function formatDate(iso: string | null) {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString(undefined, {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
}

// ── Skeleton row ──────────────────────────────────────────────────────────────

function SkeletonRow() {
  return (
    <TableRow className="animate-pulse">
      <TableCell className="px-6 py-3.5">
        <div className="h-3 w-8 rounded bg-gray-200 dark:bg-gray-700" />
      </TableCell>
      <TableCell className="px-4 py-3.5">
        <div className="flex items-center gap-3">
          <div className="h-9 w-9 shrink-0 rounded-full bg-gray-200 dark:bg-gray-700" />
          <div className="space-y-2">
            <div className="h-3 w-36 rounded bg-gray-200 dark:bg-gray-700" />
            <div className="h-2.5 w-48 rounded bg-gray-100 dark:bg-gray-800" />
          </div>
        </div>
      </TableCell>
      <TableCell className="px-4 py-3.5 hidden md:table-cell">
        <div className="h-5 w-16 rounded-full bg-gray-200 dark:bg-gray-700" />
      </TableCell>
      <TableCell className="px-4 py-3.5 hidden lg:table-cell">
        <div className="h-5 w-28 rounded-full bg-gray-200 dark:bg-gray-700" />
      </TableCell>
      <TableCell className="px-4 py-3.5 hidden xl:table-cell">
        <div className="h-5 w-20 rounded-full bg-gray-200 dark:bg-gray-700" />
      </TableCell>
      <TableCell className="px-4 py-3.5 hidden xl:table-cell">
        <div className="h-3 w-28 rounded bg-gray-200 dark:bg-gray-700" />
      </TableCell>
    </TableRow>
  );
}

// ── Pagination ────────────────────────────────────────────────────────────────

interface PaginationProps {
  page: number;
  totalPages: number;
  totalElements: number;
  onPageChange: (p: number) => void;
}

function Pagination({ page, totalPages, totalElements, onPageChange }: PaginationProps) {
  const pageNumbers = Array.from({ length: totalPages }, (_, i) => i);
  const visible = pageNumbers.reduce<(number | "…")[]>((acc, i, idx, arr) => {
    if (i === 0 || i === totalPages - 1 || Math.abs(i - page) <= 1) {
      if (acc.length > 0 && typeof acc[acc.length - 1] === "number") {
        const prev = acc[acc.length - 1] as number;
        if (i > prev + 1) acc.push("…");
      }
      acc.push(i);
    }
    return acc;
  }, []);

  return (
    <div className="flex items-center justify-between border-t border-gray-100 dark:border-gray-800 px-6 py-3">
      <span className="text-xs text-gray-500 dark:text-gray-400 tabular-nums">
        Page {page + 1} of {totalPages} &middot; {totalElements} users total
      </span>
      <div className="flex items-center gap-1">
        <button
          onClick={() => onPageChange(Math.max(0, page - 1))}
          disabled={page === 0}
          className="rounded-lg border border-gray-200 px-3 py-1.5 text-xs font-medium text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed dark:border-gray-700 dark:text-gray-400 dark:hover:bg-white/5 transition-colors"
        >
          ← Prev
        </button>
        {visible.map((item, idx) =>
          item === "…" ? (
            <span key={`el-${idx}`} className="px-1.5 text-xs text-gray-400">
              …
            </span>
          ) : (
            <button
              key={item}
              onClick={() => onPageChange(item as number)}
              className={`min-w-[28px] rounded-lg border px-2 py-1.5 text-xs font-medium transition-colors ${
                page === item
                  ? "border-brand-500 bg-brand-500 text-white"
                  : "border-gray-200 text-gray-600 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-400 dark:hover:bg-white/5"
              }`}
            >
              {(item as number) + 1}
            </button>
          )
        )}
        <button
          onClick={() => onPageChange(Math.min(totalPages - 1, page + 1))}
          disabled={page >= totalPages - 1}
          className="rounded-lg border border-gray-200 px-3 py-1.5 text-xs font-medium text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed dark:border-gray-700 dark:text-gray-400 dark:hover:bg-white/5 transition-colors"
        >
          Next →
        </button>
      </div>
    </div>
  );
}

// ── Component ─────────────────────────────────────────────────────────────────

const PAGE_SIZE = 10;

export default function UsersPage() {
  const navigate = useNavigate();

  // Server-side pagination state
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Data
  const [users, setUsers] = useState<UserDto[]>([]);
  const [groupOptions, setGroupOptions] = useState<UserGroupDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Client-side filters (applied on the loaded page)
  const [query, setQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [groupFilter, setGroupFilter] = useState("ALL");

  // Load group options for filter dropdown once
  useEffect(() => {
    listGroups()
      .then((res) => setGroupOptions(res.data.data ?? []))
      .catch(() => {/* non-critical */});
  }, []);

  // Fetch page of users whenever page index changes
  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    getUsers({ page, size: PAGE_SIZE })
      .then((res) => {
        if (cancelled) return;
        const data = res.data.data;
        setUsers(data?.content ?? []);
        setTotalElements(data?.totalElements ?? 0);
        setTotalPages(data?.totalPages ?? 0);
      })
      .catch(() => {
        if (!cancelled) setError("Failed to load users. Please try again.");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => { cancelled = true; };
  }, [page]);

  // Reset to page 0 when client-side filters change
  useEffect(() => {
    setPage(0);
  }, [query, statusFilter, groupFilter]);

  // Client-side filter on current page data
  const filtered = useMemo(() => {
    return users.filter((u) => {
      if (statusFilter !== "ALL" && u.status !== statusFilter) return false;
      if (groupFilter !== "ALL" && !u.groups.includes(groupFilter)) return false;
      if (query) {
        const q = query.toLowerCase();
        if (
          !u.name.toLowerCase().includes(q) &&
          !u.email.toLowerCase().includes(q) &&
          !String(u.id).includes(q)
        )
          return false;
      }
      return true;
    });
  }, [users, query, statusFilter, groupFilter]);

  function handleRetry() {
    setError(null);
    setPage(0);
  }

  return (
    <>
      <PageMeta
        title="Users | Auth Platform"
        description="Manage user accounts, group memberships, and access control"
      />

      {/* ── Page header ── */}
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-800 dark:text-white/90">
            Users
          </h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            {loading ? "Loading…" : `${totalElements} total users`}
          </p>
        </div>
        <button className="inline-flex items-center gap-2 rounded-lg bg-brand-500 px-4 py-2 text-sm font-medium text-white hover:bg-brand-600 transition-colors">
          <UserCircleIcon className="size-4" />
          Add user
        </button>
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
            placeholder="Search by name, email or ID…"
            className="h-10 w-72 rounded-lg border border-gray-200 bg-white pl-9 pr-3 text-sm text-gray-800 placeholder-gray-400 focus:border-brand-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-white/90 dark:placeholder-gray-500"
          />
        </div>

        {/* Status filter */}
        <div className="flex items-center gap-1.5">
          <span className="text-xs font-medium text-gray-500 dark:text-gray-400">
            Status:
          </span>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="h-10 rounded-lg border border-gray-200 bg-white px-3 text-sm text-gray-700 focus:border-brand-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-gray-300"
          >
            <option value="ALL">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
            <option value="SUSPENDED">Suspended</option>
            <option value="NEW">New</option>
          </select>
        </div>

        {/* Group filter */}
        <div className="flex items-center gap-1.5">
          <span className="text-xs font-medium text-gray-500 dark:text-gray-400">
            Group:
          </span>
          <select
            value={groupFilter}
            onChange={(e) => setGroupFilter(e.target.value)}
            className="h-10 rounded-lg border border-gray-200 bg-white px-3 text-sm text-gray-700 focus:border-brand-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-gray-300"
          >
            <option value="ALL">All groups</option>
            {groupOptions.map((g) => (
              <option key={g.id} value={g.name}>
                {g.name}
              </option>
            ))}
          </select>
        </div>

        <span className="ml-auto text-xs text-gray-400 dark:text-gray-500 tabular-nums">
          {loading ? "—" : `${filtered.length} of ${users.length} on this page`}
        </span>
      </div>

      {/* ── Error banner ── */}
      {error && !loading && (
        <div className="mb-4 flex items-center justify-between rounded-xl border border-error-200 bg-error-50 px-5 py-3 text-sm text-error-700 dark:border-error-500/20 dark:bg-error-500/10 dark:text-error-400">
          {error}
          <button
            onClick={handleRetry}
            className="ml-4 font-medium underline hover:no-underline"
          >
            Retry
          </button>
        </div>
      )}

      {/* ── Table ── */}
      <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] overflow-hidden">
        <div className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow className="border-b border-gray-100 dark:border-gray-800">
                <TableCell
                  isHeader
                  className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500 dark:text-gray-400 w-20"
                >
                  ID
                </TableCell>
                <TableCell
                  isHeader
                  className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500 dark:text-gray-400"
                >
                  User
                </TableCell>
                <TableCell
                  isHeader
                  className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500 dark:text-gray-400 hidden md:table-cell"
                >
                  Status
                </TableCell>
                <TableCell
                  isHeader
                  className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500 dark:text-gray-400 hidden lg:table-cell"
                >
                  Groups
                </TableCell>
                <TableCell
                  isHeader
                  className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500 dark:text-gray-400 hidden xl:table-cell"
                >
                  Provider
                </TableCell>
                <TableCell
                  isHeader
                  className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500 dark:text-gray-400 hidden xl:table-cell"
                >
                  Last Login
                </TableCell>
              </TableRow>
            </TableHeader>

            <TableBody className="divide-y divide-gray-100 dark:divide-gray-800">
              {loading ? (
                Array.from({ length: PAGE_SIZE }).map((_, i) => (
                  <SkeletonRow key={i} />
                ))
              ) : filtered.length === 0 ? (
                <TableRow>
                  <div className="px-6 py-12 w-full text-center text-sm text-gray-400 dark:text-gray-500">
                    <div className="flex flex-row items-center gap-2">
                      <GroupIcon className="size-8 text-gray-300 dark:text-gray-600" />
                      {error
                        ? "Could not load users."
                        : "No users match the current filters."}
                    </div>
                  </div>
                </TableRow>
              ) : (
                filtered.map((user) => (
                  <TableRow
                    key={user.id}
                    className="hover:bg-gray-50 dark:hover:bg-white/[0.02] cursor-pointer transition-colors"
                    onClick={() => navigate(`/users/${user.id}`)}
                  >
                    {/* ID */}
                    <TableCell className="px-6 py-3.5 font-mono text-xs text-gray-500 dark:text-gray-400">
                      #{user.id}
                    </TableCell>

                    {/* User */}
                    <TableCell className="px-4 py-3.5">
                      <div className="flex items-center gap-3">
                        <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-brand-50 dark:bg-brand-500/15 text-brand-600 dark:text-brand-400 text-sm font-semibold">
                          {getInitials(user.name)}
                        </div>
                        <div className="min-w-0">
                          <p className="text-sm font-medium text-gray-800 dark:text-white/90 truncate">
                            {user.name}
                          </p>
                          <p className="text-xs text-gray-500 dark:text-gray-400 truncate">
                            {user.email}
                          </p>
                        </div>
                      </div>
                    </TableCell>

                    {/* Status */}
                    <TableCell className="px-4 py-3.5 hidden md:table-cell">
                      <Badge color={STATUS_COLOR[user.status] ?? "light"} size="sm">
                        {user.status}
                      </Badge>
                    </TableCell>

                    {/* Groups */}
                    <TableCell className="px-4 py-3.5 hidden lg:table-cell">
                      <div className="flex flex-wrap gap-1">
                        {user.groups.length === 0 ? (
                          <span className="text-xs text-gray-400">—</span>
                        ) : (
                          <>
                            {user.groups.slice(0, 2).map((g) => (
                              <Badge key={g} color="primary" size="sm">
                                {g.replace(/_/g, " ")}
                              </Badge>
                            ))}
                            {user.groups.length > 2 && (
                              <Badge color="light" size="sm">
                                +{user.groups.length - 2}
                              </Badge>
                            )}
                          </>
                        )}
                      </div>
                    </TableCell>

                    {/* Provider */}
                    <TableCell className="px-4 py-3.5 hidden xl:table-cell">
                      <Badge
                        color={user.authProvider === "AZURE_AD" ? "primary" : "light"}
                        size="sm"
                      >
                        {AUTH_PROVIDER_LABEL[user.authProvider] ?? user.authProvider}
                      </Badge>
                    </TableCell>

                    {/* Last Login */}
                    <TableCell className="px-4 py-3.5 text-xs font-mono text-gray-500 dark:text-gray-400 hidden xl:table-cell">
                      {formatDate(user.lastLoginAt)}
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </div>

        {/* ── Pagination controls ── */}
        {!loading && !error && totalPages > 1 && (
          <Pagination
            page={page}
            totalPages={totalPages}
            totalElements={totalElements}
            onPageChange={setPage}
          />
        )}
      </div>
    </>
  );
}
