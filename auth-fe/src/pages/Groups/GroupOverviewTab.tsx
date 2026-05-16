import { memo } from "react";
import Badge from "../../components/ui/badge/Badge";
import { BoltIcon, LockIcon } from "../../icons";
import type { PermissionDto, RoleDto } from "../../types/admin";

interface Props {
  roles: RoleDto[];
  allPermissions: PermissionDto[];
  permsByCategory: Record<string, PermissionDto[]>;
  onAssignRole: () => void;
  onRemoveRole: (roleId: number) => void;
}

function GroupOverviewTab({
  roles,
  allPermissions,
  permsByCategory,
  onAssignRole,
  onRemoveRole,
}: Readonly<Props>) {
  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
      {/* Assigned roles */}
      <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] overflow-hidden">
        <div className="flex items-center justify-between border-b border-gray-100 dark:border-gray-800 px-5 py-4">
          <h3 className="text-sm font-semibold text-gray-800 dark:text-white/90">
            Assigned roles
          </h3>
          <button
            onClick={onAssignRole}
            className="text-xs text-brand-500 hover:text-brand-600 font-medium"
          >
            + Add
          </button>
        </div>
        {roles.length === 0 ? (
          <div className="flex flex-col items-center py-10 text-gray-400">
            <LockIcon className="size-8 mb-2 text-gray-300 dark:text-gray-700" />
            <p className="text-sm">No roles assigned</p>
          </div>
        ) : (
          <div className="divide-y divide-gray-100 dark:divide-gray-800">
            {roles.map((r) => (
              <div
                key={r.id}
                className="flex items-center gap-3 px-5 py-3 hover:bg-gray-50 dark:hover:bg-white/[0.02] transition-colors"
              >
                <LockIcon className="size-4 text-warning-500 shrink-0" />
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium font-mono text-gray-800 dark:text-white/90">
                    {r.name}
                  </p>
                  <p className="text-xs text-gray-500 dark:text-gray-400 truncate">
                    {r.description}
                  </p>
                </div>
                <span className="text-xs text-gray-400 shrink-0">
                  {r.permissions.length} perm
                  {r.permissions.length !== 1 ? "s" : ""}
                </span>
                <button
                  onClick={() => onRemoveRole(r.id)}
                  className="ml-1 text-xs text-gray-400 hover:text-error-500 dark:hover:text-error-400 transition-colors"
                  title="Remove role"
                >
                  ✕
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Effective permissions by category */}
      <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] overflow-hidden">
        <div className="border-b border-gray-100 dark:border-gray-800 px-5 py-4">
          <h3 className="text-sm font-semibold text-gray-800 dark:text-white/90">
            Effective permissions
          </h3>
          <p className="mt-0.5 text-xs text-gray-500 dark:text-gray-400">
            {allPermissions.length} permissions across{" "}
            {Object.keys(permsByCategory).length} categories
          </p>
        </div>
        {allPermissions.length === 0 ? (
          <div className="flex flex-col items-center py-10 text-gray-400">
            <BoltIcon className="size-8 mb-2 text-gray-300 dark:text-gray-700" />
            <p className="text-sm">No permissions</p>
          </div>
        ) : (
          <div className="p-5 space-y-4">
            {Object.entries(permsByCategory).map(([category, perms]) => (
              <div key={category}>
                <div className="flex items-center gap-2 mb-2">
                  <Badge color="light" size="sm">
                    {category}
                  </Badge>
                  <span className="text-xs tabular-nums text-gray-400">
                    {perms.length}
                  </span>
                </div>
                <div className="flex flex-wrap gap-1.5">
                  {perms.map((p) => (
                    <span
                      key={p.id}
                      className="inline-flex items-center gap-1 rounded-md border border-gray-200 dark:border-gray-700 px-2 py-1 text-xs font-mono text-gray-600 dark:text-gray-400"
                      title={p.description}
                    >
                      <BoltIcon className="size-2.5 text-brand-500 shrink-0" />
                      {p.code}
                    </span>
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export default memo(GroupOverviewTab);
