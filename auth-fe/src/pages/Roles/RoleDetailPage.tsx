import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router";
import PageMeta from "../../components/common/PageMeta";
import Badge from "../../components/ui/badge/Badge";
import { ChevronLeftIcon, LockIcon, BoltIcon, GroupIcon } from "../../icons";
import {
  getRole,
  listPermissions,
  assignPermissionToRole,
  removePermissionFromRole,
  listGroups,
} from "../../services/adminService";
import type { BankingRoleDto, PermissionDto, UserGroupDto } from "../../types/admin";

// ── Helpers ───────────────────────────────────────────────────────────────────

const TYPE_COLOR: Record<string, "primary" | "success" | "warning" | "error" | "light"> = {
  CUSTOMER: "primary",
  STAFF: "success",
  OVERSIGHT: "warning",
  ADMIN: "error",
};

// ── Component ─────────────────────────────────────────────────────────────────

export default function RoleDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const roleId = Number(id);

  const [role, setRole] = useState<BankingRoleDto | null>(null);
  const [allPermissions, setAllPermissions] = useState<PermissionDto[]>([]);
  const [usedByGroups, setUsedByGroups] = useState<UserGroupDto[]>([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [saveSuccess, setSaveSuccess] = useState(false);

  // Local grant set — tracks unsaved changes
  const [grantedIds, setGrantedIds] = useState<Set<number>>(new Set());
  // Original grant set — to compute diff on save
  const [originalIds, setOriginalIds] = useState<Set<number>>(new Set());

  useEffect(() => {
    if (!roleId) return;
    setLoading(true);
    setError(null);

    Promise.all([
      getRole(roleId),
      listPermissions(),
      listGroups(),
    ])
      .then(([roleRes, permsRes, groupsRes]) => {
        const r = roleRes.data.data;
        const perms = permsRes.data.data ?? [];
        const groups = groupsRes.data.data ?? [];

        if (!r) { setError("Role not found."); return; }

        setRole(r);
        setAllPermissions(perms);

        const currentIds = new Set(r.permissions.map((p) => p.id));
        setGrantedIds(new Set(currentIds));
        setOriginalIds(new Set(currentIds));

        // Groups that have this role assigned
        setUsedByGroups(groups.filter((g) => g.roles.some((gr) => gr.id === roleId)));
      })
      .catch(() => setError("Failed to load role."))
      .finally(() => setLoading(false));
  }, [roleId]);

  function togglePermission(permId: number) {
    setGrantedIds((prev) => {
      const next = new Set(prev);
      if (next.has(permId)) next.delete(permId);
      else next.add(permId);
      return next;
    });
    setSaveSuccess(false);
    setSaveError(null);
  }

  function toggleCategory(categoryPerms: PermissionDto[]) {
    const allGranted = categoryPerms.every((p) => grantedIds.has(p.id));
    setGrantedIds((prev) => {
      const next = new Set(prev);
      if (allGranted) {
        categoryPerms.forEach((p) => next.delete(p.id));
      } else {
        categoryPerms.forEach((p) => next.add(p.id));
      }
      return next;
    });
    setSaveSuccess(false);
    setSaveError(null);
  }

  async function handleSave() {
    setSaving(true);
    setSaveError(null);
    setSaveSuccess(false);

    const toAdd = [...grantedIds].filter((id) => !originalIds.has(id));
    const toRemove = [...originalIds].filter((id) => !grantedIds.has(id));

    try {
      await Promise.all([
        ...toAdd.map((permId) => assignPermissionToRole(roleId, permId)),
        ...toRemove.map((permId) => removePermissionFromRole(roleId, permId)),
      ]);
      setOriginalIds(new Set(grantedIds));
      setSaveSuccess(true);
    } catch {
      setSaveError("Some changes could not be saved. Please try again.");
    } finally {
      setSaving(false);
    }
  }

  function handleReset() {
    setGrantedIds(new Set(originalIds));
    setSaveError(null);
    setSaveSuccess(false);
  }

  const hasChanges =
    grantedIds.size !== originalIds.size ||
    [...grantedIds].some((id) => !originalIds.has(id));

  if (loading) {
    return (
      <div className="flex items-center justify-center py-24 text-sm text-gray-400">
        Loading…
      </div>
    );
  }

  if (error || !role) {
    return (
      <div className="flex flex-col items-center justify-center py-24 text-gray-400">
        <LockIcon className="size-12 mb-3 text-gray-300 dark:text-gray-700" />
        <p className="text-base font-medium">{error ?? "Role not found"}</p>
        <button
          onClick={() => navigate("/roles")}
          className="mt-4 text-sm text-brand-500 hover:text-brand-600"
        >
          Back to Roles
        </button>
      </div>
    );
  }

  // Group permissions by category
  const byCategory = allPermissions.reduce<Record<string, PermissionDto[]>>(
    (acc, p) => {
      if (!acc[p.category]) acc[p.category] = [];
      acc[p.category].push(p);
      return acc;
    },
    {}
  );

  return (
    <>
      <PageMeta
        title={`${role.name} | Roles | Auth Platform`}
        description={`Role permissions matrix for ${role.name}`}
      />

      {/* ── Back nav ── */}
      <button
        onClick={() => navigate("/roles")}
        className="mb-4 inline-flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200 transition-colors"
      >
        <ChevronLeftIcon className="size-4" />
        Roles
      </button>

      {/* ── Role header ── */}
      <div className="mb-4 rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] px-6 py-5">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start">
          <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl border border-warning-200 dark:border-warning-500/30 bg-warning-50 dark:bg-warning-500/10">
            <LockIcon className="size-6 text-warning-500" />
          </div>
          <div className="flex-1 min-w-0">
            <h1 className="text-xl font-semibold font-mono text-gray-800 dark:text-white/90">
              {role.name}
            </h1>
            <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
              {role.description}
            </p>
            <div className="mt-2 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-gray-400 dark:text-gray-500">
              <span>{grantedIds.size} permissions</span>
              <span>&middot;</span>
              <span>
                used by {usedByGroups.length} group
                {usedByGroups.length !== 1 ? "s" : ""}
              </span>
            </div>
          </div>
          <div className="flex flex-wrap gap-2 shrink-0">
            <button className="rounded-lg border border-gray-200 px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-white/5 transition-colors">
              Edit
            </button>
            <button className="rounded-lg border border-error-200 px-3 py-2 text-sm font-medium text-error-600 hover:bg-error-50 dark:border-error-500/30 dark:text-error-400 dark:hover:bg-error-500/10 transition-colors">
              Delete
            </button>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">

        {/* ── Permissions matrix ── */}
        <div className="lg:col-span-2 rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] overflow-hidden">
          <div className="flex items-center justify-between border-b border-gray-100 dark:border-gray-800 px-6 py-4">
            <div>
              <h3 className="text-sm font-semibold text-gray-800 dark:text-white/90">
                Permissions matrix
              </h3>
              <p className="mt-0.5 text-xs text-gray-500 dark:text-gray-400">
                Toggle to grant or revoke permissions
              </p>
            </div>
            <div className="flex items-center gap-2">
              {hasChanges && (
                <button
                  onClick={handleReset}
                  className="rounded-lg border border-gray-200 px-3 py-1.5 text-xs font-medium text-gray-600 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-400 dark:hover:bg-white/5 transition-colors"
                >
                  Reset
                </button>
              )}
              <button
                onClick={handleSave}
                disabled={!hasChanges || saving}
                className="rounded-lg bg-brand-500 px-3 py-1.5 text-xs font-medium text-white hover:bg-brand-600 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
              >
                {saving ? "Saving…" : "Save changes"}
              </button>
            </div>
          </div>

          {/* Feedback banners */}
          {saveSuccess && (
            <div className="mx-6 mt-4 rounded-lg border border-success-200 bg-success-50 px-4 py-2 text-sm text-success-700 dark:bg-success-500/10 dark:border-success-500/20 dark:text-success-400">
              Permissions saved successfully.
            </div>
          )}
          {saveError && (
            <div className="mx-6 mt-4 rounded-lg border border-error-200 bg-error-50 px-4 py-2 text-sm text-error-700 dark:bg-error-500/10 dark:border-error-500/20 dark:text-error-400">
              {saveError}
            </div>
          )}

          <div className="p-5 space-y-3">
            {Object.entries(byCategory).map(([category, perms]) => {
              const grantedCount = perms.filter((p) => grantedIds.has(p.id)).length;
              const allGranted = grantedCount === perms.length;

              return (
                <div
                  key={category}
                  className="rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden"
                >
                  {/* Category header */}
                  <div className="flex items-center justify-between border-b border-gray-100 dark:border-gray-800 bg-gray-50 dark:bg-white/[0.02] px-4 py-2.5">
                    <div className="flex items-center gap-2">
                      <Badge color="light" size="sm">{category}</Badge>
                      <span className="text-xs text-gray-500 dark:text-gray-400 tabular-nums">
                        {grantedCount} / {perms.length}
                      </span>
                    </div>
                    <button
                      onClick={() => toggleCategory(perms)}
                      className="text-xs font-medium text-brand-500 hover:text-brand-600 dark:text-brand-400 transition-colors"
                    >
                      {allGranted ? "Clear all" : "Select all"}
                    </button>
                  </div>

                  {/* Permission rows */}
                  <div className="divide-y divide-gray-100 dark:divide-gray-800">
                    {perms.map((p) => {
                      const granted = grantedIds.has(p.id);
                      return (
                        <label
                          key={p.id}
                          className="flex cursor-pointer items-center gap-3 px-4 py-2.5 hover:bg-gray-50 dark:hover:bg-white/[0.02] transition-colors"
                        >
                          <input
                            type="checkbox"
                            checked={granted}
                            onChange={() => togglePermission(p.id)}
                            className="h-4 w-4 rounded accent-brand-500 cursor-pointer"
                          />
                          <span
                            className={`inline-flex items-center gap-1 rounded-md border px-2 py-0.5 text-xs font-mono transition-colors ${
                              granted
                                ? "border-brand-200 bg-brand-50 text-brand-700 dark:border-brand-500/30 dark:bg-brand-500/10 dark:text-brand-300"
                                : "border-gray-200 bg-gray-50 text-gray-500 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-500"
                            }`}
                          >
                            <BoltIcon className="size-2.5 shrink-0" />
                            {p.code}
                          </span>
                          <span className="text-xs text-gray-600 dark:text-gray-400 truncate">
                            {p.description}
                          </span>
                        </label>
                      );
                    })}
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* ── Used by groups ── */}
        <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] overflow-hidden self-start">
          <div className="border-b border-gray-100 dark:border-gray-800 px-5 py-4">
            <h3 className="text-sm font-semibold text-gray-800 dark:text-white/90">
              Used by groups
            </h3>
            <p className="mt-0.5 text-xs text-gray-500 dark:text-gray-400">
              {usedByGroups.length} group{usedByGroups.length !== 1 ? "s" : ""}
            </p>
          </div>
          {usedByGroups.length === 0 ? (
            <div className="flex flex-col items-center py-10 text-gray-400">
              <GroupIcon className="size-8 mb-2 text-gray-300 dark:text-gray-700" />
              <p className="text-xs">No groups use this role</p>
            </div>
          ) : (
            <div className="divide-y divide-gray-100 dark:divide-gray-800">
              {usedByGroups.map((g) => (
                <button
                  key={g.id}
                  onClick={() => navigate(`/groups/${g.id}`)}
                  className="flex w-full items-center gap-2.5 px-5 py-3 text-left hover:bg-gray-50 dark:hover:bg-white/[0.02] transition-colors"
                >
                  <Badge color={TYPE_COLOR[g.type] ?? "light"} size="sm">
                    {g.type}
                  </Badge>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-gray-800 dark:text-white/90 truncate">
                      {g.name}
                    </p>
                    <p className="text-xs text-gray-500 dark:text-gray-400 truncate">
                      {g.description}
                    </p>
                  </div>
                  <ChevronLeftIcon className="size-4 text-gray-400 rotate-180 shrink-0" />
                </button>
              ))}
            </div>
          )}
        </div>
      </div>
    </>
  );
}
