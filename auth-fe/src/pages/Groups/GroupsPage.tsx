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
import { GroupIcon } from "../../icons";
import { listGroups } from "../../services/adminService";
import type { UserGroupDto } from "../../types/admin";
import { USERS } from "../../temp_data/rbacData";

// ── Helpers ───────────────────────────────────────────────────────────────────

type GroupType = "ALL" | "CUSTOMER" | "STAFF" | "OVERSIGHT" | "ADMIN";

const TYPE_COLOR: Record<string, "primary" | "success" | "warning" | "error" | "light"> = {
  CUSTOMER: "primary",
  STAFF: "success",
  OVERSIGHT: "warning",
  ADMIN: "error",
};

const GROUP_TYPES: GroupType[] = ["ALL", "CUSTOMER", "STAFF", "OVERSIGHT", "ADMIN"];

// ── Component ─────────────────────────────────────────────────────────────────

export default function GroupsPage() {
  const navigate = useNavigate();

  const [groups, setGroups] = useState<UserGroupDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [typeFilter, setTypeFilter] = useState<GroupType>("ALL");
  const [query, setQuery] = useState("");

  useEffect(() => {
    listGroups()
      .then((res) => setGroups(res.data.data ?? []))
      .catch(() => setError("Failed to load groups."))
      .finally(() => setLoading(false));
  }, []);

  // Enrich each group with member count (from temp data, matched by name)
  const enriched = useMemo(
    () =>
      groups.map((g) => ({
        ...g,
        memberCount: USERS.filter((u) => u.groups.includes(g.name)).length,
        permissionCount: new Set(
          g.roles.flatMap((r) => r.permissions.map((p) => p.code))
        ).size,
      })),
    [groups]
  );

  const filtered = useMemo(
    () =>
      enriched.filter((g) => {
        if (typeFilter !== "ALL" && g.type !== typeFilter) return false;
        if (query) {
          const q = query.toLowerCase();
          if (
            !g.name.toLowerCase().includes(q) &&
            !g.description.toLowerCase().includes(q)
          )
            return false;
        }
        return true;
      }),
    [enriched, typeFilter, query]
  );

  return (
    <>
      <PageMeta
        title="Groups | Auth Platform"
        description="Manage user groups and their role assignments"
      />

      {/* ── Page header ── */}
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-800 dark:text-white/90">
            Groups
          </h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            {groups.length} groups &middot; 4 types
          </p>
        </div>
        <button className="inline-flex items-center gap-2 rounded-lg bg-brand-500 px-4 py-2 text-sm font-medium text-white hover:bg-brand-600 transition-colors">
          <GroupIcon className="size-4" />
          New group
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
            placeholder="Search groups…"
            className="h-10 w-72 rounded-lg border border-gray-200 bg-white pl-9 pr-3 text-sm text-gray-800 placeholder-gray-400 focus:border-brand-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-white/90 dark:placeholder-gray-500"
          />
        </div>

        {/* Type pill filter */}
        <div className="flex items-center gap-0.5 rounded-lg bg-gray-100 dark:bg-gray-800 p-1">
          {GROUP_TYPES.map((t) => (
            <button
              key={t}
              onClick={() => setTypeFilter(t)}
              className={`h-8 rounded-md px-3 text-xs font-semibold uppercase tracking-wide transition-colors ${
                typeFilter === t
                  ? "bg-white dark:bg-gray-700 text-gray-900 dark:text-white shadow-sm"
                  : "text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200"
              }`}
            >
              {t}
            </button>
          ))}
        </div>

        <span className="ml-auto text-xs text-gray-400 dark:text-gray-500 tabular-nums">
          {filtered.length} of {groups.length}
        </span>
      </div>

      {/* ── Table ── */}
      <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center py-16 text-sm text-gray-400">
            Loading groups…
          </div>
        ) : error ? (
          <div className="flex items-center justify-center py-16 text-sm text-error-600 dark:text-error-400">
            {error}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow className="border-b border-gray-100 dark:border-gray-800">
                  {[
                    { label: "Group", className: "" },
                    { label: "Type", className: "w-32" },
                    { label: "Roles", className: "w-24 text-right" },
                    { label: "Permissions", className: "w-32 text-right hidden lg:table-cell" },
                    { label: "Members", className: "w-28 text-right hidden md:table-cell" },
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
                {filtered.length === 0 ? (
                  <TableRow>
                    <TableCell className="px-6 py-12 text-center text-sm text-gray-400 dark:text-gray-500">
                      <div className="flex flex-col items-center gap-2">
                        <GroupIcon className="size-8 text-gray-300 dark:text-gray-600" />
                        No groups match the current filters
                      </div>
                    </TableCell>
                  </TableRow>
                ) : (
                  filtered.map((g) => (
                    <TableRow
                      key={g.id}
                      className="hover:bg-gray-50 dark:hover:bg-white/[0.02] cursor-pointer transition-colors"
                      onClick={() => navigate(`/groups/${g.id}`)}
                    >
                      {/* Group name + description */}
                      <TableCell className="px-6 py-3">
                        <div className="flex items-center gap-3">
                          <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800">
                            <GroupIcon className="size-4 text-gray-500 dark:text-gray-400" />
                          </div>
                          <div className="min-w-0">
                            <p className="text-sm font-medium text-gray-800 dark:text-white/90">
                              {g.name}
                            </p>
                            <p className="text-xs text-gray-500 dark:text-gray-400 truncate max-w-xs">
                              {g.description}
                            </p>
                          </div>
                        </div>
                      </TableCell>

                      {/* Type */}
                      <TableCell className="px-6 py-3">
                        <Badge color={TYPE_COLOR[g.type] ?? "light"} size="sm">
                          {g.type}
                        </Badge>
                      </TableCell>

                      {/* Roles */}
                      <TableCell className="px-6 py-3 text-right text-sm font-medium text-gray-700 dark:text-gray-300 tabular-nums">
                        {g.roles.length}
                      </TableCell>

                      {/* Permissions */}
                      <TableCell className="px-6 py-3 text-right text-sm text-gray-600 dark:text-gray-400 tabular-nums hidden lg:table-cell">
                        {g.permissionCount}
                      </TableCell>

                      {/* Members */}
                      <TableCell className="px-6 py-3 text-right text-sm font-medium text-gray-700 dark:text-gray-300 tabular-nums hidden md:table-cell">
                        {g.memberCount}
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </div>
        )}
      </div>
    </>
  );
}
