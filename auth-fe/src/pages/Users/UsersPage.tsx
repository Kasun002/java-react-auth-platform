import { useMemo, useState } from "react";
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
import { USERS, GROUPS } from "../../temp_data/rbacData";

// ── Helpers ───────────────────────────────────────────────────────────────────

type StatusColor = "success" | "error" | "warning" | "light";

const STATUS_COLOR: Record<string, StatusColor> = {
  ACTIVE: "success",
  INACTIVE: "light",
  SUSPENDED: "error",
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

// ── Component ─────────────────────────────────────────────────────────────────

export default function UsersPage() {
  const navigate = useNavigate();

  const [query, setQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [groupFilter, setGroupFilter] = useState("ALL");

  const filtered = useMemo(() => {
    return USERS.filter((u) => {
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
  }, [query, statusFilter, groupFilter]);

  const activeCount = USERS.filter((u) => u.status === "ACTIVE").length;

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
            {USERS.length} total &middot; {activeCount} active
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
            {GROUPS.map((g) => (
              <option key={g.id} value={g.name}>
                {g.name}
              </option>
            ))}
          </select>
        </div>

        <span className="ml-auto text-xs text-gray-400 dark:text-gray-500 tabular-nums">
          {filtered.length} of {USERS.length}
        </span>
      </div>

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
                  Department
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
              {filtered.length === 0 ? (
                <TableRow>
                  <TableCell className="px-6 py-12 text-center text-sm text-gray-400 dark:text-gray-500">
                    <div className="flex flex-col items-center gap-2">
                      <GroupIcon className="size-8 text-gray-300 dark:text-gray-600" />
                      No users match the current filters
                    </div>
                  </TableCell>
                </TableRow>
              ) : (
                filtered.map((user) => (
                  <TableRow
                    key={user.id}
                    className="hover:bg-gray-50 dark:hover:bg-white/[0.02] cursor-pointer transition-colors"
                    onClick={() => navigate(`/users/${user.id}`)}
                  >
                    {/* ID */}
                    <TableCell className="px-6 py-3 font-mono text-xs text-gray-500 dark:text-gray-400">
                      #{user.id}
                    </TableCell>

                    {/* User */}
                    <TableCell className="px-4 py-3">
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
                    <TableCell className="px-4 py-3 hidden md:table-cell">
                      <Badge color={STATUS_COLOR[user.status] ?? "light"} size="sm">
                        {user.status}
                      </Badge>
                    </TableCell>

                    {/* Groups */}
                    <TableCell className="px-4 py-3 hidden lg:table-cell">
                      <div className="flex flex-wrap gap-1">
                        {user.groups.slice(0, 2).map((g) => (
                          <Badge key={g} color="primary" size="sm">
                            {g.replace(/([A-Z])/g, " $1").trim().split(" ").slice(0, 2).join(" ")}
                          </Badge>
                        ))}
                        {user.groups.length > 2 && (
                          <Badge color="light" size="sm">
                            +{user.groups.length - 2}
                          </Badge>
                        )}
                      </div>
                    </TableCell>

                    {/* Department */}
                    <TableCell className="px-4 py-3 text-sm text-gray-600 dark:text-gray-400 hidden xl:table-cell">
                      {user.department}
                    </TableCell>

                    {/* Last Login */}
                    <TableCell className="px-4 py-3 text-xs font-mono text-gray-500 dark:text-gray-400 hidden xl:table-cell">
                      {formatDate(user.lastLoginAt)}
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </div>
      </div>
    </>
  );
}
