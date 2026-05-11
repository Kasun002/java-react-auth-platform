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
import {
  listGroups,
  createGroup,
  updateGroup,
  deleteGroup,
} from "../../services/adminService";
import type { UserGroupDto, CreateGroupRequest, UpdateGroupRequest } from "../../types/admin";

// ── Helpers ───────────────────────────────────────────────────────────────────

type BadgeColor = "primary" | "success" | "warning" | "error" | "light";

const PRESET_COLORS: Record<string, BadgeColor> = {
  CUSTOMER: "primary",
  STAFF: "success",
  OVERSIGHT: "warning",
  ADMIN: "error",
};

function typeColor(type: string): BadgeColor {
  return PRESET_COLORS[type.toUpperCase()] ?? "light";
}

function apiError(err: unknown): string {
  const e = err as { response?: { data?: { message?: string } } };
  return e?.response?.data?.message ?? "An unexpected error occurred";
}

// ── Group Modal ───────────────────────────────────────────────────────────────

interface GroupModalProps {
  initial?: UserGroupDto;
  onClose: () => void;
  onSaved: (g: UserGroupDto) => void;
}

function GroupModal({ initial, onClose, onSaved }: GroupModalProps) {
  const [name, setName] = useState(initial?.name ?? "");
  const [type, setType] = useState(initial?.type ?? "");
  const [description, setDescription] = useState(initial?.description ?? "");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      if (initial) {
        const req: UpdateGroupRequest = { name, type, description };
        const res = await updateGroup(initial.id, req);
        onSaved(res.data.data!);
      } else {
        const req: CreateGroupRequest = { name, type, description };
        const res = await createGroup(req);
        onSaved(res.data.data!);
      }
      onClose();
    } catch (err) {
      setError(apiError(err));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="w-full max-w-md rounded-2xl border border-gray-200 bg-white shadow-xl dark:border-gray-800 dark:bg-gray-900">
        <div className="flex items-center justify-between border-b border-gray-100 dark:border-gray-800 px-6 py-4">
          <h3 className="text-base font-semibold text-gray-800 dark:text-white/90">
            {initial ? "Edit group" : "New group"}
          </h3>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300">✕</button>
        </div>

        <form onSubmit={handleSubmit} className="px-6 py-4 space-y-4">
          {error && (
            <div className="rounded-lg border border-error-200 bg-error-50 px-4 py-2 text-sm text-error-700 dark:bg-error-500/10 dark:border-error-500/20 dark:text-error-400">
              {error}
            </div>
          )}

          <div>
            <label className="mb-1.5 block text-xs font-medium text-gray-600 dark:text-gray-400">
              Name <span className="text-error-500">*</span>
            </label>
            <input
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. OPERATIONS_TEAM"
              className="w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 placeholder-gray-400 focus:border-brand-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-white/90"
            />
          </div>

          <div>
            <label className="mb-1.5 block text-xs font-medium text-gray-600 dark:text-gray-400">
              Type <span className="text-error-500">*</span>
            </label>
            <input
              required
              value={type}
              onChange={(e) => setType(e.target.value)}
              placeholder="e.g. STAFF"
              className="w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 placeholder-gray-400 focus:border-brand-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-white/90"
            />
          </div>

          <div>
            <label className="mb-1.5 block text-xs font-medium text-gray-600 dark:text-gray-400">
              Description
            </label>
            <input
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Brief description of this group…"
              className="w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 placeholder-gray-400 focus:border-brand-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-white/90"
            />
          </div>

          <div className="flex justify-end gap-2 pt-2">
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
              {saving ? "Saving…" : initial ? "Save changes" : "Create"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ── Main Component ─────────────────────────────────────────────────────────────

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

  // Derive types dynamically from loaded groups
  const types = useMemo(
    () => [...new Set(groups.map((g) => g.type))].sort(),
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

  async function handleDelete(g: UserGroupDto, e: React.MouseEvent) {
    e.stopPropagation();
    if (!window.confirm(`Delete group "${g.name}"? This cannot be undone.`)) return;
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

      {/* ── Page header ── */}
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-800 dark:text-white/90">
            Groups
          </h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            {groups.length} groups &middot; {types.length} type{types.length !== 1 ? "s" : ""}
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

      {/* ── Delete error banner ── */}
      {deleteError && (
        <div className="mb-4 rounded-lg border border-error-200 bg-error-50 px-4 py-3 text-sm text-error-700 dark:bg-error-500/10 dark:border-error-500/20 dark:text-error-400">
          {deleteError}
          <button onClick={() => setDeleteError(null)} className="ml-3 text-xs underline">Dismiss</button>
        </div>
      )}

      {/* ── Filters ── */}
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
            placeholder="Search groups…"
            className="h-10 w-72 rounded-lg border border-gray-200 bg-white pl-9 pr-3 text-sm text-gray-800 placeholder-gray-400 focus:border-brand-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-white/90 dark:placeholder-gray-500"
          />
        </div>

        {/* Dynamic type pill filter */}
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
                    { label: "", className: "w-24" },
                  ].map(({ label, className }) => (
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
                        <Badge color={typeColor(g.type)} size="sm">
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

                      {/* Actions */}
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
        )}
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
