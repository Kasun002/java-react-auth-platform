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
import { GroupIcon, UserCircleIcon } from "../../icons";
import { getUsers, listGroups } from "../../services/adminService";
import type { UserGroupDto } from "../../types/admin";
import type { UserDto } from "../../types/auth";
import Pagination from "../../utils/tblPagination";
import CreateUserModal from "./CreateUserModal";
import {
  AUTH_PROVIDER_LABEL,
  PAGE_SIZE,
  STATUS_COLOR,
  formatDate,
  getInitials,
} from "./userUtils";

// Skeleton row — tightly coupled to this table's column structure
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

const HEADER_CELLS = [
  { label: "ID", className: "px-6 w-20" },
  { label: "User", className: "px-4" },
  { label: "Status", className: "px-4 hidden md:table-cell" },
  { label: "Groups", className: "px-4 hidden lg:table-cell" },
  { label: "Provider", className: "px-4 hidden xl:table-cell" },
  { label: "Last Login", className: "px-4 hidden xl:table-cell" },
];

export default function UsersPage() {
  const navigate = useNavigate();

  const [createOpen, setCreateOpen] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [users, setUsers] = useState<UserDto[]>([]);
  const [groupOptions, setGroupOptions] = useState<UserGroupDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [groupFilter, setGroupFilter] = useState("ALL");

  useEffect(() => {
    listGroups()
      .then((res) => setGroupOptions(res.data.data ?? []))
      .catch(() => { /* non-critical */ });
  }, []);

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
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [page, refreshKey]);

  useEffect(() => { setPage(0); }, [query, statusFilter, groupFilter]);

  const filtered = useMemo(
    () =>
      users.filter((u) => {
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
      }),
    [users, query, statusFilter, groupFilter]
  );

  return (
    <>
      <PageMeta
        title="Users | Auth Platform"
        description="Manage user accounts, group memberships, and access control"
      />

      {/* Page header */}
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-800 dark:text-white/90">Users</h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            {loading ? "Loading…" : `${totalElements} total users`}
          </p>
        </div>
        <button
          onClick={() => setCreateOpen(true)}
          className="inline-flex items-center gap-2 rounded-lg bg-brand-500 px-4 py-2 text-sm font-medium text-white hover:bg-brand-600 transition-colors"
        >
          <UserCircleIcon className="size-4" />
          Add user
        </button>
      </div>

      {/* Filters */}
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <div className="relative">
          <svg
            className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"
            width="16" height="16" viewBox="0 0 24 24" fill="none"
            stroke="currentColor" strokeWidth="2"
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

        <div className="flex items-center gap-1.5">
          <span className="text-xs font-medium text-gray-500 dark:text-gray-400">Status:</span>
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

        <div className="flex items-center gap-1.5">
          <span className="text-xs font-medium text-gray-500 dark:text-gray-400">Group:</span>
          <select
            value={groupFilter}
            onChange={(e) => setGroupFilter(e.target.value)}
            className="h-10 rounded-lg border border-gray-200 bg-white px-3 text-sm text-gray-700 focus:border-brand-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-gray-300"
          >
            <option value="ALL">All groups</option>
            {groupOptions.map((g) => (
              <option key={g.id} value={g.name}>{g.name}</option>
            ))}
          </select>
        </div>

        <span className="ml-auto text-xs text-gray-400 dark:text-gray-500 tabular-nums">
          {loading ? "—" : `${filtered.length} of ${users.length} on this page`}
        </span>
      </div>

      {/* Error banner */}
      {error && !loading && (
        <div className="mb-4 flex items-center justify-between rounded-xl border border-error-200 bg-error-50 px-5 py-3 text-sm text-error-700 dark:border-error-500/20 dark:bg-error-500/10 dark:text-error-400">
          {error}
          <button
            onClick={() => { setError(null); setPage(0); }}
            className="ml-4 font-medium underline hover:no-underline"
          >
            Retry
          </button>
        </div>
      )}

      {/* Table */}
      <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] overflow-hidden">
        <div className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow className="border-b border-gray-100 dark:border-gray-800">
                {HEADER_CELLS.map(({ label, className }) => (
                  <TableCell
                    key={label}
                    isHeader
                    className={`${className} py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500 dark:text-gray-400`}
                  >
                    {label}
                  </TableCell>
                ))}
              </TableRow>
            </TableHeader>

            <TableBody className="divide-y divide-gray-100 dark:divide-gray-800">
              {loading ? (
                Array.from({ length: PAGE_SIZE }).map((_, i) => <SkeletonRow key={i} />)
              ) : filtered.length === 0 ? (
                <TableRow>
                  <div className="px-6 py-12 w-full text-center text-sm text-gray-400 dark:text-gray-500">
                    <div className="flex flex-row items-center gap-2">
                      <GroupIcon className="size-8 text-gray-300 dark:text-gray-600" />
                      {error ? "Could not load users." : "No users match the current filters."}
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
                    <TableCell className="px-6 py-3.5 font-mono text-xs text-gray-500 dark:text-gray-400">
                      #{user.id}
                    </TableCell>
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
                    <TableCell className="px-4 py-3.5 hidden md:table-cell">
                      <Badge color={STATUS_COLOR[user.status] ?? "light"} size="sm">
                        {user.status}
                      </Badge>
                    </TableCell>
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
                              <Badge color="light" size="sm">+{user.groups.length - 2}</Badge>
                            )}
                          </>
                        )}
                      </div>
                    </TableCell>
                    <TableCell className="px-4 py-3.5 hidden xl:table-cell">
                      <Badge
                        color={user.authProvider === "AZURE_AD" ? "primary" : "light"}
                        size="sm"
                      >
                        {AUTH_PROVIDER_LABEL[user.authProvider] ?? user.authProvider}
                      </Badge>
                    </TableCell>
                    <TableCell className="px-4 py-3.5 text-xs font-mono text-gray-500 dark:text-gray-400 hidden xl:table-cell">
                      {formatDate(user.lastLoginAt)}
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </div>

        {!loading && !error && totalPages > 1 && (
          <Pagination
            page={page}
            totalPages={totalPages}
            totalElements={totalElements}
            onPageChange={setPage}
          />
        )}
      </div>

      {createOpen && (
        <CreateUserModal
          onClose={() => setCreateOpen(false)}
          onCreated={() => { setPage(0); setRefreshKey((k) => k + 1); }}
        />
      )}
    </>
  );
}
