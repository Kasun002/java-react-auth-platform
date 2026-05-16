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
import { deleteGroup, listGroups } from "../../services/adminService";
import type { UserGroupDto } from "../../types/admin";
import GroupModal from "./GroupModal";
import { apiError, typeColor } from "./groupUtils";
import SearchInput from "../../components/ui/SearchInput";

const TABLE_COLUMNS = [
  { label: "Group", className: "" },
  { label: "Type", className: "w-32" },
  { label: "Roles", className: "w-24 text-right" },
  { label: "Permissions", className: "w-32 text-right hidden lg:table-cell" },
  { label: "", className: "w-24" },
];

export default function GroupsPage() {
  const navigate = useNavigate();

  const [groups, setGroups] = useState<UserGroupDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [typeFilter, setTypeFilter] = useState("ALL");
  const [query, setQuery] = useState("");
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<UserGroupDto | undefined>(undefined);

  useEffect(() => {
    listGroups()
      .then((res) => setGroups(res.data.data ?? []))
      .catch(() => setError("Failed to load groups."))
      .finally(() => setLoading(false));
  }, []);

  const types = useMemo(
    () =>
      [...new Set(groups.map((g) => g.type))].sort((a, b) =>
        a.localeCompare(b)
      ),
    [groups]
  );

  const enriched = useMemo(
    () =>
      groups.map((g) => ({
        ...g,
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
            !(g.description ?? "").toLowerCase().includes(q)
          )
            return false;
        }
        return true;
      }),
    [enriched, typeFilter, query]
  );

  function openCreate() {
    setEditing(undefined);
    setModalOpen(true);
  }

  function openEdit(g: UserGroupDto, e: React.MouseEvent) {
    e.stopPropagation();
    setEditing(g);
    setModalOpen(true);
  }

  function handleSaved(saved: UserGroupDto) {
    setGroups((prev) => {
      const idx = prev.findIndex((g) => g.id === saved.id);
      if (idx >= 0) {
        const next = [...prev];
        next[idx] = saved;
        return next;
      }
      return [...prev, saved];
    });
  }

  function renderContent() {
    if (loading) {
      return (
        <div className="flex items-center justify-center py-16 text-sm text-gray-400">
          Loading groups…
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
                  <TableCell className="px-6 py-3">
                    <Badge color={typeColor(g.type)} size="sm">
                      {g.type}
                    </Badge>
                  </TableCell>
                  <TableCell className="px-6 py-3 text-right text-sm font-medium text-gray-700 dark:text-gray-300 tabular-nums">
                    {g.roles.length}
                  </TableCell>
                  <TableCell className="px-6 py-3 text-right text-sm text-gray-600 dark:text-gray-400 tabular-nums hidden lg:table-cell">
                    {g.permissionCount}
                  </TableCell>
                  <TableCell className="px-6 py-3">
                    <div className="flex items-center justify-end gap-1">
                      <button
                        onClick={(e) => openEdit(g, e)}
                        className="rounded px-2 py-1 text-xs font-medium text-gray-500 hover:text-gray-800 hover:bg-gray-100 dark:text-gray-400 dark:hover:text-white dark:hover:bg-white/5 transition-colors"
                      >
                        Edit
                      </button>
                      <button
                        onClick={(e) => handleDelete(g, e)}
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

  async function handleDelete(g: UserGroupDto, e: React.MouseEvent) {
    e.stopPropagation();
    if (!globalThis.confirm(`Delete group "${g.name}"? This cannot be undone.`))
      return;
    setDeleteError(null);
    try {
      await deleteGroup(g.id);
      setGroups((prev) => prev.filter((x) => x.id !== g.id));
    } catch (err) {
      setDeleteError(apiError(err));
    }
  }

  return (
    <>
      <PageMeta
        title="Groups | Auth Platform"
        description="Manage user groups and their role assignments"
      />

      {/* Page header */}
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-800 dark:text-white/90">
            Groups
          </h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            {groups.length} groups &middot; {types.length} type
            {types.length === 1 ? "" : "s"}
          </p>
        </div>
        <button
          onClick={openCreate}
          className="inline-flex items-center gap-2 rounded-lg bg-brand-500 px-4 py-2 text-sm font-medium text-white hover:bg-brand-600 transition-colors"
        >
          <GroupIcon className="size-4" />
          New group
        </button>
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
        <SearchInput value={query} onChange={setQuery} placeholder="Search groups…" />

        <div className="flex flex-wrap items-center gap-0.5 rounded-lg bg-gray-100 dark:bg-gray-800 p-1">
          {["ALL", ...types].map((t) => (
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

      {/* Table */}
      <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] overflow-hidden">
        {renderContent()}
      </div>

      {modalOpen && (
        <GroupModal
          initial={editing}
          onClose={() => setModalOpen(false)}
          onSaved={handleSaved}
        />
      )}
    </>
  );
}
