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
import {
  getUsers,
  listGroups,
  createUser,
} from "../../services/adminService";
import type {
  AdminCreateUserRequest,
  UserGroupDto,
} from "../../types/admin";
import type { UserDto } from "../../types/auth";
import Pagination from "../../utils/tblPagination";

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

// ── Create User Modal ─────────────────────────────────────────────────────────

interface CreateUserModalProps {
  onClose: () => void;
  onCreated: () => void;
}

function CreateUserModal({ onClose, onCreated }: Readonly<CreateUserModalProps>) {
  const [form, setForm] = useState<AdminCreateUserRequest>({
    name: "",
    email: "",
    temporaryPassword: "",
    phone: "",
    groupIds: [],
  });
  const [groups, setGroups] = useState<UserGroupDto[]>([]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listGroups()
      .then((r) => setGroups(r.data.data ?? []))
      .catch(() => {});
  }, []);

  function toggle<T>(arr: T[], val: T): T[] {
    return arr.includes(val) ? arr.filter((x) => x !== val) : [...arr, val];
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await createUser({ ...form, phone: form.phone || undefined });
      onCreated();
      onClose();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })
        ?.response?.data?.message;
      setError(msg ?? "Failed to create user. Please try again.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="w-full max-w-2xl rounded-2xl border border-gray-200 bg-white shadow-xl dark:border-gray-800 dark:bg-gray-900 flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-gray-100 dark:border-gray-800 px-6 py-4 shrink-0">
          <h3 className="text-base font-semibold text-gray-800 dark:text-white/90">
            Create user
          </h3>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
          >
            ✕
          </button>
        </div>

        {/* Body */}
        <form onSubmit={handleSubmit} className="flex flex-col flex-1 min-h-0">
          <div className="overflow-y-auto flex-1 px-6 py-4 space-y-4">
            {error && (
              <div className="rounded-lg border border-error-200 bg-error-50 px-4 py-2 text-sm text-error-700 dark:bg-error-500/10 dark:border-error-500/20 dark:text-error-400">
                {error}
              </div>
            )}

            {/* Profile fields */}
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div>
                <label className="mb-1.5 block text-xs font-semibold text-gray-600 dark:text-gray-400">
                  Full name <span className="text-error-500">*</span>
                </label>
                <input
                  required
                  value={form.name}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, name: e.target.value }))
                  }
                  placeholder="Jane Doe"
                  className="w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 focus:border-brand-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-white/90"
                />
              </div>
              <div>
                <label className="mb-1.5 block text-xs font-semibold text-gray-600 dark:text-gray-400">
                  Email <span className="text-error-500">*</span>
                </label>
                <input
                  required
                  type="email"
                  value={form.email}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, email: e.target.value }))
                  }
                  placeholder="jane@corp.example.com"
                  className="w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 focus:border-brand-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-white/90"
                />
              </div>
              <div>
                <label className="mb-1.5 block text-xs font-semibold text-gray-600 dark:text-gray-400">
                  Temporary password <span className="text-error-500">*</span>
                </label>
                <input
                  required
                  type="password"
                  value={form.temporaryPassword}
                  onChange={(e) =>
                    setForm((f) => ({
                      ...f,
                      temporaryPassword: e.target.value,
                    }))
                  }
                  placeholder="Min 8 chars, upper/lower/digit/symbol"
                  className="w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 focus:border-brand-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-white/90"
                />
              </div>
              <div>
                <label className="mb-1.5 block text-xs font-semibold text-gray-600 dark:text-gray-400">
                  Phone
                </label>
                <input
                  value={form.phone}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, phone: e.target.value }))
                  }
                  placeholder="+94771234567"
                  className="w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 focus:border-brand-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-white/90"
                />
              </div>
            </div>

            {/* Groups */}
            <div>
              <label className="mb-1.5 block text-xs font-semibold text-gray-600 dark:text-gray-400">
                Groups{" "}
                <span className="text-gray-400 font-normal">(optional)</span>
              </label>
              <div className="max-h-36 overflow-y-auto rounded-xl border border-gray-200 dark:border-gray-700">
                {groups.length === 0 ? (
                  <p className="px-4 py-3 text-xs text-gray-400">
                    No groups available
                  </p>
                ) : (
                  groups.map((g) => (
                    <label
                      key={g.id}
                      className="flex cursor-pointer items-center gap-3 border-b border-gray-100 dark:border-gray-800 last:border-0 px-4 py-2.5 hover:bg-gray-50 dark:hover:bg-white/[0.02]"
                    >
                      <input
                        type="checkbox"
                        checked={form.groupIds.includes(g.id)}
                        onChange={() =>
                          setForm((f) => ({
                            ...f,
                            groupIds: toggle(f.groupIds, g.id),
                          }))
                        }
                        className="accent-brand-500"
                      />
                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-medium text-gray-800 dark:text-white/90">
                          {g.name}
                        </p>
                        <p className="truncate text-xs text-gray-400">
                          {g.description}
                        </p>
                      </div>
                      <Badge color="light" size="sm">
                        {g.type}
                      </Badge>
                    </label>
                  ))
                )}
              </div>
            </div>

          </div>

          {/* Footer */}
          <div className="flex justify-end gap-2 border-t border-gray-100 dark:border-gray-800 px-6 py-4 shrink-0">
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg border border-gray-200 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-white/5"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={saving}
              className="rounded-lg bg-brand-500 px-4 py-2 text-sm font-medium text-white hover:bg-brand-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {saving ? "Creating…" : "Create user"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ── Component ─────────────────────────────────────────────────────────────────

const PAGE_SIZE = 10;

export default function UsersPage() {
  const navigate = useNavigate();

  const [createOpen, setCreateOpen] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);

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
      .catch(() => {
        /* non-critical */
      });
  }, []);

  // Fetch page of users whenever page index or refreshKey changes
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
    return () => {
      cancelled = true;
    };
  }, [page, refreshKey]);

  // Reset to page 0 when client-side filters change
  useEffect(() => {
    setPage(0);
  }, [query, statusFilter, groupFilter]);

  // Client-side filter on current page data
  const filtered = useMemo(() => {
    return users.filter((u) => {
      if (statusFilter !== "ALL" && u.status !== statusFilter) return false;
      if (groupFilter !== "ALL" && !u.groups.includes(groupFilter))
        return false;
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
        <button
          onClick={() => setCreateOpen(true)}
          className="inline-flex items-center gap-2 rounded-lg bg-brand-500 px-4 py-2 text-sm font-medium text-white hover:bg-brand-600 transition-colors"
        >
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
                      <Badge
                        color={STATUS_COLOR[user.status] ?? "light"}
                        size="sm"
                      >
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
                        color={
                          user.authProvider === "AZURE_AD" ? "primary" : "light"
                        }
                        size="sm"
                      >
                        {AUTH_PROVIDER_LABEL[user.authProvider] ??
                          user.authProvider}
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

      {/* ── Create user modal ── */}
      {createOpen && (
        <CreateUserModal
          onClose={() => setCreateOpen(false)}
          onCreated={() => {
            setPage(0);
            setRefreshKey((k) => k + 1);
          }}
        />
      )}
    </>
  );
}
