import { useEffect, useState } from "react";
import Badge from "../../components/ui/badge/Badge";
import { addUserToGroup, listGroups } from "../../services/adminService";
import type { UserGroupDto } from "../../types/admin";

interface Props {
  userId: number;
  currentGroupIds: number[];
  onClose: () => void;
  onAssigned: () => void;
}

export default function AssignGroupModal({
  userId,
  currentGroupIds,
  onClose,
  onAssigned,
}: Readonly<Props>) {
  const [availableGroups, setAvailableGroups] = useState<UserGroupDto[]>([]);
  const [selected, setSelected] = useState<number | null>(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listGroups()
      .then((res) => {
        const all = res.data.data ?? [];
        setAvailableGroups(all.filter((g) => !currentGroupIds.includes(g.id)));
      })
      .catch(() => setError("Failed to load groups."));
  }, [currentGroupIds]);

  async function handleAssign() {
    if (!selected) return;
    setSaving(true);
    setError(null);
    try {
      await addUserToGroup(userId, selected);
      onAssigned();
      onClose();
    } catch {
      setError("Failed to assign group. Please try again.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="w-full max-w-lg rounded-2xl border border-gray-200 bg-white shadow-xl dark:border-gray-800 dark:bg-gray-900">
        <div className="flex items-center justify-between border-b border-gray-100 dark:border-gray-800 px-6 py-4">
          <h3 className="text-base font-semibold text-gray-800 dark:text-white/90">
            Assign to group
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
            Adding the user to a group grants them all permissions from the
            group&apos;s roles. Effective permissions update on next login.
          </p>

          {error && (
            <div className="mb-3 rounded-lg border border-error-200 bg-error-50 px-4 py-2 text-sm text-error-700 dark:bg-error-500/10 dark:border-error-500/20 dark:text-error-400">
              {error}
            </div>
          )}

          <div className="max-h-72 overflow-y-auto rounded-xl border border-gray-200 dark:border-gray-700">
            {availableGroups.length === 0 ? (
              <p className="px-4 py-6 text-center text-sm text-gray-400">
                No available groups
              </p>
            ) : (
              availableGroups.map((g) => (
                <label
                  key={g.id}
                  className={`flex cursor-pointer items-center gap-3 border-b border-gray-100 dark:border-gray-800 last:border-0 px-4 py-3 hover:bg-gray-50 dark:hover:bg-white/[0.02] transition-colors ${
                    selected === g.id ? "bg-brand-50 dark:bg-brand-500/10" : ""
                  }`}
                >
                  <input
                    type="radio"
                    name="group-select"
                    checked={selected === g.id}
                    onChange={() => setSelected(g.id)}
                    className="accent-brand-500"
                  />
                  <Badge color="light" size="sm">{g.type}</Badge>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-gray-800 dark:text-white/90">
                      {g.name}
                    </p>
                    <p className="truncate text-xs text-gray-500 dark:text-gray-400">
                      {g.description}
                    </p>
                  </div>
                  <span className="shrink-0 text-xs text-gray-400">
                    {g.roles.length} role{g.roles.length !== 1 ? "s" : ""}
                  </span>
                </label>
              ))
            )}
          </div>
        </div>

        <div className="flex justify-end gap-2 border-t border-gray-100 dark:border-gray-800 px-6 py-4">
          <button
            onClick={onClose}
            className="rounded-lg border border-gray-200 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-white/5"
          >
            Cancel
          </button>
          <button
            onClick={handleAssign}
            disabled={!selected || saving}
            className="rounded-lg bg-brand-500 px-4 py-2 text-sm font-medium text-white hover:bg-brand-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {saving ? "Assigning…" : "Assign group"}
          </button>
        </div>
      </div>
    </div>
  );
}
