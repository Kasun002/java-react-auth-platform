import { useEffect, useMemo, useState } from "react";
import {
  createRole,
  listPermissions,
  updateRole,
} from "../../services/adminService";
import type {
  CreateRoleRequest,
  PermissionDto,
  RoleDto,
  UpdateRoleRequest,
} from "../../types/admin";
import { apiError } from "./roleUtils";

interface Props {
  initial?: RoleDto;
  onClose: () => void;
  onSaved: (r: RoleDto) => void;
}

export default function RoleModal({
  initial,
  onClose,
  onSaved,
}: Readonly<Props>) {
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
        const saved = res.data.data;
        if (saved) {
          onSaved(saved);
          onClose();
        }
      } else {
        const req: CreateRoleRequest = {
          name,
          description,
          permissionIds: selectedPermIds,
        };
        const res = await createRole(req);
        const saved = res.data.data;
        if (saved) {
          onSaved(saved);
          onClose();
        }
      }
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
              <div className="mb-1.5 text-xs font-medium text-gray-600 dark:text-gray-400">
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
              <div className="mb-1.5 text-xs font-medium text-gray-600 dark:text-gray-400">
                Description
              </div>
              <input
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="What this role grants…"
                className="w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 placeholder-gray-400 focus:border-brand-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-white/90"
              />
            </div>

            <div>
              <div className="mb-1.5 flex items-center justify-between">
                <span className="text-xs font-medium text-gray-600 dark:text-gray-400">
                  Permissions{" "}
                  <span className="text-gray-400 font-normal">(optional)</span>
                </span>
                {selectedPermIds.length > 0 && (
                  <span className="text-xs text-brand-500 font-medium">
                    {selectedPermIds.length} selected
                  </span>
                )}
              </div>
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
                          <div
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
                          </div>
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
