import { useState } from "react";
import { updateGroup } from "../../services/adminService";
import type { UpdateGroupRequest, UserGroupDto } from "../../types/admin";
import {
  apiError,
  cancelBtnCls,
  errorBannerCls,
  inputCls,
  saveBtnCls,
} from "./groupUtils";

interface Props {
  group: UserGroupDto;
  onClose: () => void;
  onSaved: (g: UserGroupDto) => void;
}

export default function EditGroupModal({
  group,
  onClose,
  onSaved,
}: Readonly<Props>) {
  const [name, setName] = useState(group.name);
  const [type, setType] = useState(group.type);
  const [description, setDescription] = useState(group.description ?? "");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      // Preserve existing roleIds so metadata edits don't wipe role assignments
      const req: UpdateGroupRequest = {
        name,
        type,
        description,
        roleIds: group.roles.map((r) => r.id),
      };
      const saved = (await updateGroup(group.id, req)).data.data;
      if (saved) {
        onSaved(saved);
        onClose();
      }
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
            Edit group
          </h3>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
          >
            ✕
          </button>
        </div>
        <form onSubmit={handleSubmit} className="px-6 py-4 space-y-4">
          {error && <div className={errorBannerCls}>{error}</div>}

          <div>
            <div className="mb-1.5 block text-xs font-medium text-gray-600 dark:text-gray-400">
              Name <span className="text-error-500">*</span>
            </div>
            <input
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              className={inputCls}
            />
          </div>

          <div>
            <div className="mb-1.5 block text-xs font-medium text-gray-600 dark:text-gray-400">
              Type <span className="text-error-500">*</span>
            </div>
            <input
              required
              value={type}
              onChange={(e) => setType(e.target.value)}
              className={inputCls}
            />
          </div>

          <div>
            <div className="mb-1.5 block text-xs font-medium text-gray-600 dark:text-gray-400">
              Description
            </div>
            <input
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className={inputCls}
            />
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <button type="button" onClick={onClose} className={cancelBtnCls}>
              Cancel
            </button>
            <button type="submit" disabled={saving} className={saveBtnCls}>
              {saving ? "Saving…" : "Save changes"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
