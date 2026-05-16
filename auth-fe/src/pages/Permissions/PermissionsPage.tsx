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
import {
  deletePermission,
  listPermissions,
  listRoles,
} from "../../services/adminService";
import type { PermissionDto, RoleDto } from "../../types/admin";
import PermissionModal from "./PermissionModal";
import { apiError, TABLE_COLUMNS } from "./permissionUtils";

export default function PermissionsPage() {
  const [permissions, setPermissions] = useState<PermissionDto[]>([]);
  const [roles, setRoles] = useState<RoleDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [catFilter, setCatFilter] = useState("ALL");
  const [query, setQuery] = useState("");
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<PermissionDto | undefined>(undefined);
  const [deleteError, setDeleteError] = useState<string | null>(null);

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
    () =>
      [...new Set(permissions.map((p) => p.category))].sort((a, b) =>
        a.localeCompare(b)
      ),
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
            !(p.description ?? "").toLowerCase().includes(q)
          )
            return false;
        }
        return true;
      }),
    [permissions, catFilter, query]
  );

  const roleCountMap = useMemo(() => {
    const map: Record<number, number> = {};
    roles.forEach((r) => {
      r.permissions.forEach((p) => {
        map[p.id] = (map[p.id] ?? 0) + 1;
      });
    });
    return map;
  }, [roles]);

  function openCreate() {
    setEditing(undefined);
    setModalOpen(true);
  }

  function openEdit(p: PermissionDto) {
    setEditing(p);
    setModalOpen(true);
  }

  function handleSaved(saved: PermissionDto) {
    setPermissions((prev) => {
      const idx = prev.findIndex((p) => p.id === saved.id);
      if (idx >= 0) {
        const next = [...prev];
        next[idx] = saved;
        return next;
      }
      return [...prev, saved];
    });
  }

  async function handleDelete(p: PermissionDto) {
    if (
      !globalThis.confirm(
        `Delete permission "${p.code}"? This cannot be undone.`
      )
    )
      return;
    setDeleteError(null);
    try {
      await deletePermission(p.id);
      setPermissions((prev) => prev.filter((x) => x.id !== p.id));
    } catch (err) {
      setDeleteError(apiError(err));
    }
  }

  function renderContent() {
    if (loading) {
      return (
        <div className="flex items-center justify-center py-16 text-sm text-gray-400">
          Loading permissions…
        </div>
      );
    }
    if (error) {
      return (
        <div className="flex items-center justify-center py-16 text-sm text-error-600 dark:text-error-400">
          {error}
        </div>
      );
    }
    return (
      <div className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow className="border-b border-gray-100 dark:border-gray-800">
              {TABLE_COLUMNS.map(({ label, className }) => (
                <TableCell
                  key={label || "_actions"}
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
                  <TableCell className="px-6 py-3">
                    <span className="inline-flex items-center gap-1.5 rounded-md border border-brand-200 bg-brand-50 dark:border-brand-500/30 dark:bg-brand-500/10 px-2.5 py-1 text-xs font-mono text-brand-700 dark:text-brand-300">
                      <BoltIcon className="size-3 shrink-0" />
                      {p.code}
                    </span>
                  </TableCell>
                  <TableCell className="px-6 py-3 hidden md:table-cell">
                    <Badge color="light" size="sm">
                      {p.category}
                    </Badge>
                  </TableCell>
                  <TableCell className="px-6 py-3 text-sm text-gray-600 dark:text-gray-400">
                    {p.description}
                  </TableCell>
                  <TableCell className="px-6 py-3 text-right text-sm tabular-nums text-gray-600 dark:text-gray-400 hidden lg:table-cell">
                    {roleCountMap[p.id] ?? 0}
                  </TableCell>
                  <TableCell className="px-6 py-3">
                    <div className="flex items-center justify-end gap-1">
                      <button
                        onClick={() => openEdit(p)}
                        className="rounded px-2 py-1 text-xs font-medium text-gray-500 hover:text-gray-800 hover:bg-gray-100 dark:text-gray-400 dark:hover:text-white dark:hover:bg-white/5 transition-colors"
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => handleDelete(p)}
                        className="rounded px-2 py-1 text-xs font-medium text-error-500 hover:text-error-700 hover:bg-error-50 dark:hover:bg-error-500/10 transition-colors"
                      >
                        Delete
                      </button>
                    </div>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>
    );
  }

  return (
    <>
      <PageMeta
        title="Permissions | Auth Platform"
        description="Permission catalog — all atomic operation codes across resource categories"
      />

      {/* Page header */}
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-800 dark:text-white/90">
            Permissions
          </h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            {permissions.length} permission codes across {categories.length}{" "}
            categories
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Badge color="light" size="sm">
            OWASP ASVS L2
          </Badge>
          <Badge color="light" size="sm">
            PCI-DSS Req 7.2
          </Badge>
          <button
            onClick={openCreate}
            className="inline-flex items-center gap-2 rounded-lg bg-brand-500 px-4 py-2 text-sm font-medium text-white hover:bg-brand-600 transition-colors"
          >
            <BoltIcon className="size-4" />
            New permission
          </button>
        </div>
      </div>

      {/* Delete error banner */}
      {deleteError && (
        <div className="mb-4 rounded-lg border border-error-200 bg-error-50 px-4 py-3 text-sm text-error-700 dark:bg-error-500/10 dark:border-error-500/20 dark:text-error-400">
          {deleteError}
          <button
            onClick={() => setDeleteError(null)}
            className="ml-3 text-xs underline"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Filters */}
      <div className="mb-4 flex flex-wrap items-center gap-3">
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
              {c === "ALL" ? c : c.split(" ")[0]}
            </button>
          ))}
        </div>

        <span className="ml-auto text-xs text-gray-400 dark:text-gray-500 tabular-nums">
          {filtered.length} of {permissions.length}
        </span>
      </div>

      {/* Table */}
      <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] overflow-hidden">
        {renderContent()}
      </div>

      {modalOpen && (
        <PermissionModal
          initial={editing}
          onClose={() => setModalOpen(false)}
          onSaved={handleSaved}
        />
      )}
    </>
  );
}
