import { useEffect, useState } from "react";
import {
  createGroup,
  listRoles,
  updateGroup,
} from "../../services/adminService";
import type {
  CreateGroupRequest,
  RoleDto,
  UpdateGroupRequest,
  UserGroupDto,
} from "../../types/admin";
import {
  apiError,
  cancelBtnCls,
  errorBannerCls,
  inputCls,
  saveBtnCls,
} from "./groupUtils";

interface Props {
  initial?: UserGroupDto;
  onClose: () => void;
  onSaved: (g: UserGroupDto) => void;
}

export default function GroupModal({
  initial,
  onClose,
  onSaved,
}: Readonly<Props>) {
  const [name, setName] = useState(initial?.name ?? "");
  const [type, setType] = useState(initial?.type ?? "");
  const [description, setDescription] = useState(initial?.description ?? "");
  const [selectedRoleIds, setSelectedRoleIds] = useState<number[]>(
    initial?.roles.map((r) => r.id) ?? []
  );
  const [availableRoles, setAvailableRoles] = useState<RoleDto[]>([]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listRoles()
      .then((res) => setAvailableRoles(res.data.data ?? []))
      .catch(() => {});
  }, []);

  function toggleRole(id: number) {
    setSelectedRoleIds((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]
    );
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      if (initial) {
        const req: UpdateGroupRequest = {
          name,
          type,
          description,
          roleIds: selectedRoleIds,
        };
        const saved = (await updateGroup(initial.id, req)).data.data;
        if (saved) {
          onSaved(saved);
          onClose();
        }
      } else {
        const req: CreateGroupRequest = {
          name,
          type,
          description,
          roleIds: selectedRoleIds,
        };
        const saved = (await createGroup(req)).data.data;
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
            {initial ? "Edit group" : "New group"}
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
            {error && <div className={errorBannerCls}>{error}</div>}

            <div>
              <div className="mb-1.5 text-xs font-medium text-gray-600 dark:text-gray-400">
                Name <span className="text-error-500">*</span>
              </div>
              <input
                required
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. OPERATIONS_TEAM"
                className={inputCls}
              />
            </div>

            <div>
              <div className="mb-1.5 text-xs font-medium text-gray-600 dark:text-gray-400">
                Type <span className="text-error-500">*</span>
              </div>
              <input
                required
                value={type}
                onChange={(e) => setType(e.target.value)}
                placeholder="e.g. STAFF"
                className={inputCls}
              />
            </div>

            <div>
              <div className="mb-1.5 text-xs font-medium text-gray-600 dark:text-gray-400">
                Description
              </div>
              <input
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Brief description of this group…"
                className={inputCls}
              />
            </div>

            <div>
              <label className="mb-1.5 block text-xs font-medium text-gray-600 dark:text-gray-400">
                Roles{" "}
                <span className="text-gray-400 font-normal">
                  (optional — users inherit permissions from these roles)
                </span>
              </label>
              <div className="max-h-44 overflow-y-auto rounded-xl border border-gray-200 dark:border-gray-700">
                {availableRoles.length === 0 ? (
                  <p className="px-4 py-3 text-xs text-gray-400">
                    No roles available
                  </p>
                ) : (
                  availableRoles.map((r) => (
                    <label
                      key={r.id}
                      className="flex cursor-pointer items-center gap-3 border-b border-gray-100 dark:border-gray-800 last:border-0 px-4 py-2.5 hover:bg-gray-50 dark:hover:bg-white/[0.02]"
                    >
                      <input
                        type="checkbox"
                        checked={selectedRoleIds.includes(r.id)}
                        onChange={() => toggleRole(r.id)}
                        className="accent-brand-500"
                      />
                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-medium font-mono text-gray-800 dark:text-white/90">
                          {r.name}
                        </p>
                        {r.description && (
                          <p className="truncate text-xs text-gray-400">
                            {r.description}
                          </p>
                        )}
                      </div>
                      <span className="shrink-0 text-xs text-gray-400">
                        {r.permissions.length} perm
                        {r.permissions.length === 1 ? "" : "s"}
                      </span>
                    </label>
                  ))
                )}
              </div>
            </div>
          </div>

          <div className="flex justify-end gap-2 border-t border-gray-100 dark:border-gray-800 px-6 py-4 shrink-0">
            <button type="button" onClick={onClose} className={cancelBtnCls}>
              Cancel
            </button>
            <button type="submit" disabled={saving} className={saveBtnCls}>
              {saving ? "Saving…" : <>{initial ? "Save changes" : "Create"}</>}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
