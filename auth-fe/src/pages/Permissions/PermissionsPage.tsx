import { useEffect, useMemo, useState } from "react";
import PageMeta from "../../components/common/PageMeta";
import Badge from "../../components/ui/badge/Badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHeader,
  TableRow,
} from "../../components/ui/table";
import { BoltIcon } from "../../icons";
import { listPermissions, listRoles } from "../../services/adminService";
import type { RoleDto, PermissionDto } from "../../types/admin";

export default function PermissionsPage() {
  const [permissions, setPermissions] = useState<PermissionDto[]>([]);
  const [roles, setRoles] = useState<RoleDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [catFilter, setCatFilter] = useState("ALL");
  const [query, setQuery] = useState("");

  useEffect(() => {
    Promise.all([listPermissions(), listRoles()])
      .then(([permsRes, rolesRes]) => {
        setPermissions(permsRes.data.data ?? []);
        setRoles(rolesRes.data.data ?? []);
      })
      .catch(() => setError("Failed to load permissions."))
      .finally(() => setLoading(false));
  }, []);

  const categories = useMemo(
    () => [...new Set(permissions.map((p) => p.category))].sort(),
    [permissions]
  );

  const filtered = useMemo(
    () =>
      permissions.filter((p) => {
        if (catFilter !== "ALL" && p.category !== catFilter) return false;
        if (query) {
          const q = query.toLowerCase();
          if (
            !p.code.toLowerCase().includes(q) &&
            !p.description.toLowerCase().includes(q)
          )
            return false;
        }
        return true;
      }),
    [permissions, catFilter, query]
  );

  // Count how many roles use each permission
  const roleCountMap = useMemo(() => {
    const map: Record<number, number> = {};
    roles.forEach((r) => {
      r.permissions.forEach((p) => {
        map[p.id] = (map[p.id] ?? 0) + 1;
      });
    });
    return map;
  }, [roles]);

  return (
    <>
      <PageMeta
        title="Permissions | Auth Platform"
        description="Permission catalog — all atomic operation codes across resource categories"
      />

      {/* ── Page header ── */}
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-800 dark:text-white/90">
            Permissions
          </h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            {permissions.length} permission codes across {categories.length} categories
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Badge color="light" size="sm">OWASP ASVS L2</Badge>
          <Badge color="light" size="sm">PCI-DSS Req 7.2</Badge>
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
            placeholder="Search permissions…"
            className="h-10 w-72 rounded-lg border border-gray-200 bg-white pl-9 pr-3 text-sm text-gray-800 placeholder-gray-400 focus:border-brand-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-white/90 dark:placeholder-gray-500"
          />
        </div>

        {/* Category pill filter */}
        <div className="flex flex-wrap items-center gap-0.5 rounded-lg bg-gray-100 dark:bg-gray-800 p-1">
          {["ALL", ...categories].map((c) => (
            <button
              key={c}
              onClick={() => setCatFilter(c)}
              className={`h-8 rounded-md px-3 text-xs font-semibold uppercase tracking-wide transition-colors ${
                catFilter === c
                  ? "bg-white dark:bg-gray-700 text-gray-900 dark:text-white shadow-sm"
                  : "text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200"
              }`}
            >
              {c === "ALL" ? "ALL" : c.split(" ")[0]}
            </button>
          ))}
        </div>

        <span className="ml-auto text-xs text-gray-400 dark:text-gray-500 tabular-nums">
          {filtered.length} of {permissions.length}
        </span>
      </div>

      {/* ── Table ── */}
      <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center py-16 text-sm text-gray-400">
            Loading permissions…
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
                    { label: "Code", className: "w-64" },
                    { label: "Category", className: "w-40 hidden md:table-cell" },
                    { label: "Description", className: "" },
                    { label: "Roles", className: "w-20 text-right hidden lg:table-cell" },
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
                    <TableCell className="px-6 py-12 text-center text-sm text-gray-400">
                      <div className="flex flex-col items-center gap-2">
                        <BoltIcon className="size-8 text-gray-300 dark:text-gray-600" />
                        No permissions match the current filters
                      </div>
                    </TableCell>
                  </TableRow>
                ) : (
                  filtered.map((p) => (
                    <TableRow
                      key={p.id}
                      className="hover:bg-gray-50 dark:hover:bg-white/[0.02] transition-colors"
                    >
                      {/* Code */}
                      <TableCell className="px-6 py-3">
                        <span className="inline-flex items-center gap-1.5 rounded-md border border-brand-200 bg-brand-50 dark:border-brand-500/30 dark:bg-brand-500/10 px-2.5 py-1 text-xs font-mono text-brand-700 dark:text-brand-300">
                          <BoltIcon className="size-3 shrink-0" />
                          {p.code}
                        </span>
                      </TableCell>

                      {/* Category */}
                      <TableCell className="px-6 py-3 hidden md:table-cell">
                        <Badge color="light" size="sm">{p.category}</Badge>
                      </TableCell>

                      {/* Description */}
                      <TableCell className="px-6 py-3 text-sm text-gray-600 dark:text-gray-400">
                        {p.description}
                      </TableCell>

                      {/* Role count */}
                      <TableCell className="px-6 py-3 text-right text-sm tabular-nums text-gray-600 dark:text-gray-400 hidden lg:table-cell">
                        {roleCountMap[p.id] ?? 0}
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
