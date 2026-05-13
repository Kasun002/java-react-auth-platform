import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router";
import PageMeta from "../../components/common/PageMeta";
import {
  Table,
  TableBody,
  TableCell,
  TableHeader,
  TableRow,
} from "../../components/ui/table";
import { LockIcon } from "../../icons";
import {
  createRole,
  deleteRole,
  listPermissions,
  listRoles,
  updateRole,
} from "../../services/adminService";
import type {
  CreateRoleRequest,
  PermissionDto,
  RoleDto,
  UpdateRoleRequest,
} from "../../types/admin";

// ── Helpers ───────────────────────────────────────────────────────────────────

function apiError(err: unknown): string {
  const e = err as { response?: { data?: { message?: string } } };
  return e?.response?.data?.message ?? "An unexpected error occurred";
}

// ── Role Modal ────────────────────────────────────────────────────────────────

interface RoleModalProps {
  initial?: RoleDto;
  onClose: () => void;
  onSaved: (r: RoleDto) => void;
}

function RoleModal({ initial, onClose, onSaved }: Readonly<RoleModalProps>) {
  const [name, setName] = useState(initial?.name ?? "");
  const [description, setDescription] = useState(initial?.description ?? "");
  const [selectedPermIds, setSelectedPermIds] = useState<number[]>(
    initial?.permissions.map((p) => p.id) ?? []
  );
  const [allPermissions, setAllPermissions] = useState<PermissionDto[]>([]);
  const [permSearch, setPermSearch] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listPermissions()
      .then((res) => setAllPermissions(res.data.data ?? []))
      .catch(() => {});
  }, []);

  // Group permissions by category for a cleaner UI
  const grouped = useMemo(() => {
    const q = permSearch.toLowerCase();
    const filtered = q
      ? allPermissions.filter(
          (p) =>
            p.code.toLowerCase().includes(q) ||
            p.category.toLowerCase().includes(q) ||
            (p.description ?? "").toLowerCase().includes(q)
        )
      : allPermissions;

    return filtered.reduce<Record<string, PermissionDto[]>>((acc, p) => {
      (acc[p.category] = acc[p.category] ?? []).push(p);
      return acc;
    }, {});
  }, [allPermissions, permSearch]);

  function togglePerm(id: number) {
    setSelectedPermIds((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]
    );
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      if (initial) {
        const req: UpdateRoleRequest = {
          name,
          description,
          permissionIds: selectedPermIds,
        };
        const res = await updateRole(initial.id, req);
        onSaved(res.data.data!);
      } else {
        const req: CreateRoleRequest = {
          name,
          description,
          permissionIds: selectedPermIds,
        };
        const res = await createRole(req);
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
      <div className="w-full max-w-lg rounded-2xl border border-gray-200 bg-white shadow-xl dark:border-gray-800 dark:bg-gray-900 flex flex-col max-h-[90vh]">
        <div className="flex items-center justify-between border-b border-gray-100 dark:border-gray-800 px-6 py-4 shrink-0">
          <h3 className="text-base font-semibold text-gray-800 dark:text-white/90">
            {initial ? "Edit role" : "New role"}
          </h3>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
          >
            ✕
          </button>
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col flex-1 min-h-0">
          <div className="overflow-y-auto flex-1 px-6 py-4 space-y-4">
            {error && (
              <div className="rounded-lg border border-error-200 bg-error-50 px-4 py-2 text-sm text-error-700 dark:bg-error-500/10 dark:border-error-500/20 dark:text-error-400">
                {error}
              </div>
            )}

            <div>
              <div className="mb-1.5 block text-xs font-medium text-gray-600 dark:text-gray-400">
                Name <span className="text-error-500">*</span>
              </div>
              <input
                required
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. ROLE_MANAGER"
                className="w-full rounded-lg border border-gray-200 bg-white px-3 py-2 font-mono text-sm text-gray-800 placeholder-gray-400 focus:border-brand-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-white/90"
              />
            </div>

            <div>
              <div className="mb-1.5 block text-xs font-medium text-gray-600 dark:text-gray-400">
                Description
              </div>
              <input
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="What this role grants…"
                className="w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 placeholder-gray-400 focus:border-brand-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-white/90"
              />
            </div>

            {/* Permission assignment grouped by category */}
            <div>
              <div className="mb-1.5 flex items-center justify-between">
                <label className="text-xs font-medium text-gray-600 dark:text-gray-400">
                  Permissions{" "}
                  <span className="text-gray-400 font-normal">(optional)</span>
                </label>
                {selectedPermIds.length > 0 && (
                  <span className="text-xs text-brand-500 font-medium">
                    {selectedPermIds.length} selected
                  </span>
                )}
              </div>
              {/* Search within permissions */}
              <input
                value={permSearch}
                onChange={(e) => setPermSearch(e.target.value)}
                placeholder="Filter permissions…"
                className="mb-2 w-full rounded-lg border border-gray-200 bg-white px-3 py-1.5 text-sm text-gray-800 placeholder-gray-400 focus:border-brand-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-white/90"
              />
              <div className="max-h-52 overflow-y-auto rounded-xl border border-gray-200 dark:border-gray-700 space-y-0">
                {Object.keys(grouped).length === 0 ? (
                  <p className="px-4 py-3 text-xs text-gray-400">
                    No permissions found
                  </p>
                ) : (
                  Object.entries(grouped)
                    .sort(([a], [b]) => a.localeCompare(b))
                    .map(([category, perms]) => (
                      <div key={category}>
                        <div className="sticky top-0 bg-gray-50 dark:bg-gray-800/80 px-4 py-1.5 text-[10px] font-bold uppercase tracking-widest text-gray-400 dark:text-gray-500">
                          {category}
                        </div>
                        {perms.map((p) => (
                          <label
                            key={p.id}
                            className="flex cursor-pointer items-center gap-3 border-b border-gray-100 dark:border-gray-800 last:border-0 px-4 py-2 hover:bg-gray-50 dark:hover:bg-white/[0.02]"
                          >
                            <input
                              type="checkbox"
                              checked={selectedPermIds.includes(p.id)}
                              onChange={() => togglePerm(p.id)}
                              className="accent-brand-500"
                            />
                            <div className="min-w-0 flex-1">
                              <p className="text-xs font-medium font-mono text-gray-800 dark:text-white/90">
                                {p.code}
                              </p>
                              {p.description && (
                                <p className="truncate text-xs text-gray-400">
                                  {p.description}
                                </p>
                              )}
                            </div>
                          </label>
                        ))}
                      </div>
                    ))
                )}
              </div>
            </div>
          </div>

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
              {saving ? "Saving…" : <>{initial ? "Save changes" : "Create"}</>}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ── Main Component ────────────────────────────────────────────────────────────

export default function RolesPage() {
  const navigate = useNavigate();

  const [roles, setRoles] = useState<RoleDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<RoleDto | undefined>(undefined);

  useEffect(() => {
    listRoles()
      .then((res) => setRoles(res.data.data ?? []))
      .catch(() => setError("Failed to load roles."))
      .finally(() => setLoading(false));
  }, []);

  const totalBindings = roles.reduce((sum, r) => sum + r.permissions.length, 0);

  function openCreate() {
    setEditing(undefined);
    setModalOpen(true);
  }

  function openEdit(r: RoleDto, e: React.MouseEvent) {
    e.stopPropagation();
    setEditing(r);
    setModalOpen(true);
  }

  function handleSaved(saved: RoleDto) {
    setRoles((prev) => {
      const idx = prev.findIndex((r) => r.id === saved.id);
      if (idx >= 0) {
        const next = [...prev];
        next[idx] = saved;
        return next;
      }
      return [...prev, saved];
    });
  }

  async function handleDelete(r: RoleDto, e: React.MouseEvent) {
    e.stopPropagation();
    if (!globalThis.confirm(`Delete role "${r.name}"? This cannot be undone.`))
      return;
    setDeleteError(null);
    try {
      await deleteRole(r.id);
      setRoles((prev) => prev.filter((x) => x.id !== r.id));
    } catch (err) {
      setDeleteError(apiError(err));
    }
  }

  return (
    <>
      <PageMeta
        title="Roles | Auth Platform"
        description="Manage roles and their permission assignments"
      />

      {/* ── Page header ── */}
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-800 dark:text-white/90">
            Roles
          </h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            {roles.length} roles &middot; {totalBindings} role-permission
            bindings
          </p>
        </div>
        <button
          onClick={openCreate}
          className="inline-flex items-center gap-2 rounded-lg bg-brand-500 px-4 py-2 text-sm font-medium text-white hover:bg-brand-600 transition-colors"
        >
          <LockIcon className="size-4" />
          New role
        </button>
      </div>

      {/* ── Delete error banner ── */}
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

      {/* ── Table ── */}
      <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center py-16 text-sm text-gray-400">
            Loading roles…
          </div>
        ) : (
          <>
            {error ? (
              <div className="flex items-center justify-center py-16 text-sm text-error-600 dark:text-error-400">
                {error}
              </div>
            ) : (
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow className="border-b border-gray-100 dark:border-gray-800">
                      {[
                        { label: "Role", className: "" },
                        { label: "Permissions", className: "w-32 text-right" },
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
                    {roles.length === 0 ? (
                      <TableRow>
                        <TableCell className="px-6 py-12 text-center text-sm text-gray-400">
                          <div className="flex flex-col items-center gap-2">
                            <LockIcon className="size-8 text-gray-300 dark:text-gray-600" />
                            No roles found
                          </div>
                        </TableCell>
                      </TableRow>
                    ) : (
                      roles.map((r) => (
                        <TableRow
                          key={r.id}
                          className="hover:bg-gray-50 dark:hover:bg-white/[0.02] cursor-pointer transition-colors"
                          onClick={() => navigate(`/roles/${r.id}`)}
                        >
                          {/* Role name + description */}
                          <TableCell className="px-6 py-3">
                            <div className="flex items-center gap-3">
                              <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl border border-warning-200 dark:border-warning-500/30 bg-warning-50 dark:bg-warning-500/10">
                                <LockIcon className="size-4 text-warning-500" />
                              </div>
                              <div className="min-w-0">
                                <p className="text-sm font-medium font-mono text-gray-800 dark:text-white/90">
                                  {r.name}
                                </p>
                                <p className="text-xs text-gray-500 dark:text-gray-400 truncate max-w-sm">
                                  {r.description}
                                </p>
                              </div>
                            </div>
                          </TableCell>

                          {/* Permissions count */}
                          <TableCell className="px-6 py-3 text-right">
                            <span className="text-sm font-semibold text-gray-800 dark:text-white/90 tabular-nums">
                              {r.permissions.length}
                            </span>
                          </TableCell>

                          {/* Actions */}
                          <TableCell className="px-6 py-3">
                            <div className="flex items-center justify-end gap-1">
                              <button
                                onClick={(e) => openEdit(r, e)}
                                className="rounded px-2 py-1 text-xs font-medium text-gray-500 hover:text-gray-800 hover:bg-gray-100 dark:text-gray-400 dark:hover:text-white dark:hover:bg-white/5 transition-colors"
                              >
                                Edit
                              </button>
                              <button
                                onClick={(e) => handleDelete(r, e)}
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
          </>
        )}
      </div>

      {modalOpen && (
        <RoleModal
          initial={editing}
          onClose={() => setModalOpen(false)}
          onSaved={handleSaved}
        />
      )}
    </>
  );
}
