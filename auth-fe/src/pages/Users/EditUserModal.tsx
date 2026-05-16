import { useState } from "react";
import { updateUser } from "../../services/adminService";
import type { AdminUpdateUserRequest } from "../../types/admin";
import type { UserDto } from "../../types/auth";

interface Props {
  user: UserDto;
  onClose: () => void;
  onSaved: (updated: UserDto) => void;
}

export default function EditUserModal({ user, onClose, onSaved }: Readonly<Props>) {
  const [form, setForm] = useState<AdminUpdateUserRequest>({
    name: user.name,
    email: user.email,
    phone: user.phone ?? "",
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const res = await updateUser(user.id, { ...form, phone: form.phone || undefined });
      const saved = res.data.data;
      if (saved) { onSaved(saved); onClose(); }
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? "Failed to update user. Please try again.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="w-full max-w-lg rounded-2xl border border-gray-200 bg-white shadow-xl dark:border-gray-800 dark:bg-gray-900">
        <div className="flex items-center justify-between border-b border-gray-100 dark:border-gray-800 px-6 py-4">
          <h3 className="text-base font-semibold text-gray-800 dark:text-white/90">
            Edit user
          </h3>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
          >
            ✕
          </button>
        </div>

        <form onSubmit={handleSubmit} className="px-6 py-4 space-y-3">
          {error && (
            <div className="rounded-lg border border-error-200 bg-error-50 px-4 py-2 text-sm text-error-700 dark:bg-error-500/10 dark:border-error-500/20 dark:text-error-400">
              {error}
            </div>
          )}
          <div>
            <div className="mb-1.5 block text-xs font-semibold text-gray-600 dark:text-gray-400">
              Full name <span className="text-error-500">*</span>
            </div>
            <input
              required
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
              className="w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 focus:border-brand-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-white/90"
            />
          </div>
          <div>
            <div className="mb-1.5 block text-xs font-semibold text-gray-600 dark:text-gray-400">
              Email <span className="text-error-500">*</span>
            </div>
            <input
              required
              type="email"
              value={form.email}
              onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))}
              className="w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 focus:border-brand-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-white/90"
            />
          </div>
          <div>
            <div className="mb-1.5 block text-xs font-semibold text-gray-600 dark:text-gray-400">
              Phone
            </div>
            <input
              value={form.phone}
              onChange={(e) => setForm((f) => ({ ...f, phone: e.target.value }))}
              placeholder="+94771234567"
              className="w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 focus:border-brand-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-white/90"
            />
          </div>
        </form>

        <div className="flex justify-end gap-2 border-t border-gray-100 dark:border-gray-800 px-6 py-4">
          <button
            onClick={onClose}
            className="rounded-lg border border-gray-200 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-white/5"
          >
            Cancel
          </button>
          <button
            onClick={(e) => handleSubmit(e as unknown as React.FormEvent)}
            disabled={saving}
            className="rounded-lg bg-brand-500 px-4 py-2 text-sm font-medium text-white hover:bg-brand-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {saving ? "Saving…" : "Save changes"}
          </button>
        </div>
      </div>
    </div>
  );
}
