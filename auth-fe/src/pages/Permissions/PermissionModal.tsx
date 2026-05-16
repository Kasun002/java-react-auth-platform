import { useState } from "react";
import {
  createPermission,
  updatePermission,
} from "../../services/adminService";
import type {
  CreatePermissionRequest,
  PermissionDto,
  UpdatePermissionRequest,
} from "../../types/admin";
import { apiError } from "./permissionUtils";

interface Props {
  initial?: PermissionDto;
  onClose: () => void;
  onSaved: (p: PermissionDto) => void;
}

const inputCls =
  "w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 placeholder-gray-400 focus:border-brand-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-white/90";

const inputMonoCls = `${inputCls} font-mono`;

export default function PermissionModal({ initial, onClose, onSaved }: Readonly<Props>) {
  const [code, setCode] = useState(initial?.code ?? "");
  const [category, setCategory] = useState(initial?.category ?? "");
  const [description, setDescription] = useState(initial?.description ?? "");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      if (initial) {
        const req: UpdatePermissionRequest = { code, category, description };
        const res = await updatePermission(initial.id, req);
        const saved = res.data.data;
        if (saved) { onSaved(saved); onClose(); }
      } else {
        const req: CreatePermissionRequest = { code, category, description };
        const res = await createPermission(req);
        const saved = res.data.data;
        if (saved) { onSaved(saved); onClose(); }
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
            {initial ? "Edit permission" : "New permission"}
          </h3>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
          >
            ✕
          </button>
        </div>

        <form onSubmit={handleSubmit} className="px-6 py-4 space-y-4">
          {error && (
            <div className="rounded-lg border border-error-200 bg-error-50 px-4 py-2 text-sm text-error-700 dark:bg-error-500/10 dark:border-error-500/20 dark:text-error-400">
              {error}
            </div>
          )}

          <div>
            <div className="mb-1.5 block text-xs font-medium text-gray-600 dark:text-gray-400">
              Code <span className="text-error-500">*</span>
            </div>
            <input
              required
              value={code}
              onChange={(e) => setCode(e.target.value)}
              placeholder="e.g. INVOICE_CREATE"
              className={inputMonoCls}
            />
          </div>

          <div>
            <div className="mb-1.5 block text-xs font-medium text-gray-600 dark:text-gray-400">
              Category <span className="text-error-500">*</span>
            </div>
            <input
              required
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              placeholder="e.g. INVOICE"
              className={inputMonoCls}
            />
          </div>

          <div>
            <div className="mb-1.5 block text-xs font-medium text-gray-600 dark:text-gray-400">
              Description
            </div>
            <input
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="What this permission grants…"
              className={inputCls}
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
