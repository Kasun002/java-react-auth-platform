import { useEffect, useState } from "react";
import { LockIcon } from "../../icons";
import { assignRoleToGroup, listRoles } from "../../services/adminService";
import type { RoleDto } from "../../types/admin";
import { cancelBtnCls, errorBannerCls, saveBtnCls } from "./groupUtils";

interface Props {
  groupId: number;
  currentRoleIds: number[];
  onClose: () => void;
  onAssigned: () => void;
}

export default function AssignRoleModal({
  groupId,
  currentRoleIds,
  onClose,
  onAssigned,
}: Readonly<Props>) {
  const [availableRoles, setAvailableRoles] = useState<RoleDto[]>([]);
  const [selected, setSelected] = useState<number | null>(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listRoles()
      .then((res) => {
        const all = res.data.data ?? [];
        setAvailableRoles(all.filter((r) => !currentRoleIds.includes(r.id)));
      })
      .catch(() => setError("Failed to load roles."));
  }, [currentRoleIds]);

  async function handleAssign() {
    if (!selected) return;
    setSaving(true);
    setError(null);
    try {
      await assignRoleToGroup(groupId, selected);
      onAssigned();
      onClose();
    } catch {
      setError("Failed to assign role. Please try again.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="w-full max-w-lg rounded-2xl border border-gray-200 bg-white shadow-xl dark:border-gray-800 dark:bg-gray-900">
        <div className="flex items-center justify-between border-b border-gray-100 dark:border-gray-800 px-6 py-4">
          <h3 className="text-base font-semibold text-gray-800 dark:text-white/90">
            Assign role to group
          </h3>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
          >
            ✕
          </button>
        </div>

        <div className="px-6 py-4">
          <p className="mb-4 text-sm text-gray-500 dark:text-gray-400">
            Assigning a role grants all users in this group its permissions.
            Effective permissions update on next login.
          </p>

          {error && <div className={`mb-3 ${errorBannerCls}`}>{error}</div>}

          <div className="max-h-72 overflow-y-auto rounded-xl border border-gray-200 dark:border-gray-700">
            {availableRoles.length === 0 ? (
              <p className="px-4 py-6 text-center text-sm text-gray-400">
                No available roles
              </p>
            ) : (
              availableRoles.map((r) => (
                <div
                  key={r.id}
                  className={`flex cursor-pointer items-center gap-3 border-b border-gray-100 dark:border-gray-800 last:border-0 px-4 py-3 hover:bg-gray-50 dark:hover:bg-white/[0.02] transition-colors ${
                    selected === r.id ? "bg-brand-50 dark:bg-brand-500/10" : ""
                  }`}
                >
                  <input
                    type="radio"
                    name="role-select"
                    checked={selected === r.id}
                    onChange={() => setSelected(r.id)}
                    className="accent-brand-500"
                  />
                  <LockIcon className="size-4 text-warning-500 shrink-0" />
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium font-mono text-gray-800 dark:text-white/90">
                      {r.name}
                    </p>
                    <p className="truncate text-xs text-gray-500 dark:text-gray-400">
                      {r.description}
                    </p>
                  </div>
                  <span className="shrink-0 text-xs text-gray-400">
                    {r.permissions.length} perm
                    {r.permissions.length === 1 ? "" : "s"}
                  </span>
                </div>
              ))
            )}
          </div>
        </div>

        <div className="flex justify-end gap-2 border-t border-gray-100 dark:border-gray-800 px-6 py-4">
          <button onClick={onClose} className={cancelBtnCls}>
            Cancel
          </button>
          <button
            onClick={handleAssign}
            disabled={!selected || saving}
            className={saveBtnCls}
          >
            {saving ? "Assigning…" : "Assign role"}
          </button>
        </div>
      </div>
    </div>
  );
}
