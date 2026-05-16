import { useEffect, useState } from "react";
import Badge from "../../components/ui/badge/Badge";
import { createUser, listGroups } from "../../services/adminService";
import type { AdminCreateUserRequest, UserGroupDto } from "../../types/admin";

interface Props {
  onClose: () => void;
  onCreated: () => void;
}

function toggle<T>(arr: T[], val: T): T[] {
  return arr.includes(val) ? arr.filter((x) => x !== val) : [...arr, val];
}

export default function CreateUserModal({ onClose, onCreated }: Readonly<Props>) {
  const [form, setForm] = useState<AdminCreateUserRequest>({
    name: "",
    email: "",
    temporaryPassword: "",
    phone: "",
    groupIds: [],
  });
  const [groups, setGroups] = useState<UserGroupDto[]>([]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listGroups()
      .then((r) => setGroups(r.data.data ?? []))
      .catch(() => {});
  }, []);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await createUser({ ...form, phone: form.phone || undefined });
      onCreated();
      onClose();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })
        ?.response?.data?.message;
      setError(msg ?? "Failed to create user. Please try again.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="w-full max-w-2xl rounded-2xl border border-gray-200 bg-white shadow-xl dark:border-gray-800 dark:bg-gray-900 flex flex-col max-h-[90vh]">
        <div className="flex items-center justify-between border-b border-gray-100 dark:border-gray-800 px-6 py-4 shrink-0">
          <h3 className="text-base font-semibold text-gray-800 dark:text-white/90">
            Create user
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

            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div>
                <label className="mb-1.5 block text-xs font-semibold text-gray-600 dark:text-gray-400">
                  Full name <span className="text-error-500">*</span>
                </label>
                <input
                  required
                  value={form.name}
                  onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
                  placeholder="Jane Doe"
                  className="w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 focus:border-brand-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-white/90"
                />
              </div>
              <div>
                <label className="mb-1.5 block text-xs font-semibold text-gray-600 dark:text-gray-400">
                  Email <span className="text-error-500">*</span>
                </label>
                <input
                  required
                  type="email"
                  value={form.email}
                  onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))}
                  placeholder="jane@corp.example.com"
                  className="w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 focus:border-brand-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-white/90"
                />
              </div>
              <div>
                <label className="mb-1.5 block text-xs font-semibold text-gray-600 dark:text-gray-400">
                  Temporary password <span className="text-error-500">*</span>
                </label>
                <input
                  required
                  type="password"
                  value={form.temporaryPassword}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, temporaryPassword: e.target.value }))
                  }
                  placeholder="Min 8 chars, upper/lower/digit/symbol"
                  className="w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 focus:border-brand-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-white/90"
                />
              </div>
              <div>
                <label className="mb-1.5 block text-xs font-semibold text-gray-600 dark:text-gray-400">
                  Phone
                </label>
                <input
                  value={form.phone}
                  onChange={(e) => setForm((f) => ({ ...f, phone: e.target.value }))}
                  placeholder="+94771234567"
                  className="w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 focus:border-brand-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-white/90"
                />
              </div>
            </div>

            <div>
              <label className="mb-1.5 block text-xs font-semibold text-gray-600 dark:text-gray-400">
                Groups{" "}
                <span className="text-gray-400 font-normal">(optional)</span>
              </label>
              <div className="max-h-36 overflow-y-auto rounded-xl border border-gray-200 dark:border-gray-700">
                {groups.length === 0 ? (
                  <p className="px-4 py-3 text-xs text-gray-400">No groups available</p>
                ) : (
                  groups.map((g) => (
                    <label
                      key={g.id}
                      className="flex cursor-pointer items-center gap-3 border-b border-gray-100 dark:border-gray-800 last:border-0 px-4 py-2.5 hover:bg-gray-50 dark:hover:bg-white/[0.02]"
                    >
                      <input
                        type="checkbox"
                        checked={form.groupIds.includes(g.id)}
                        onChange={() =>
                          setForm((f) => ({ ...f, groupIds: toggle(f.groupIds, g.id) }))
                        }
                        className="accent-brand-500"
                      />
                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-medium text-gray-800 dark:text-white/90">
                          {g.name}
                        </p>
                        <p className="truncate text-xs text-gray-400">{g.description}</p>
                      </div>
                      <Badge color="light" size="sm">{g.type}</Badge>
                    </label>
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
              {saving ? "Creating…" : "Create user"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
