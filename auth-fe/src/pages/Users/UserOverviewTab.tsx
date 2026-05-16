import { memo } from "react";
import Badge from "../../components/ui/badge/Badge";
import { GroupIcon, LockIcon } from "../../icons";
import type { UserGroupDto } from "../../types/admin";
import type { UserDto } from "../../types/auth";
import {
  AUTH_PROVIDER_COLOR,
  AUTH_PROVIDER_LABEL,
  STATUS_COLOR,
  formatDateTime,
  type RoleWithSource,
} from "./userUtils";

interface ProfileRowProps {
  label: string;
  value: React.ReactNode;
}

function ProfileRow({ label, value }: ProfileRowProps) {
  return (
    <div className="grid grid-cols-[140px_1fr] items-start border-b border-gray-100 dark:border-gray-800 last:border-0 px-5 py-3">
      <dt className="text-xs font-semibold uppercase tracking-wide text-gray-400 dark:text-gray-500 pt-0.5">
        {label}
      </dt>
      <dd className="text-sm text-gray-700 dark:text-gray-300 break-all">{value}</dd>
    </div>
  );
}

interface Props {
  user: UserDto;
  groups: UserGroupDto[];
  uniqueRoles: RoleWithSource[];
  groupsLoading: boolean;
  groupsError: string | null;
  isAdUser: boolean;
  onAssignGroup: () => void;
  onRemoveGroup: (groupId: number) => void;
}

function UserOverviewTab({
  user,
  groups,
  uniqueRoles,
  groupsLoading,
  groupsError,
  isAdUser,
  onAssignGroup,
  onRemoveGroup,
}: Readonly<Props>) {
  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
      {/* Profile card */}
      <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
        <div className="border-b border-gray-100 dark:border-gray-800 px-5 py-4">
          <h3 className="text-sm font-semibold text-gray-800 dark:text-white/90">Profile</h3>
        </div>
        <dl className="py-2">
          <ProfileRow label="User ID" value={<span className="font-mono">#{user.id}</span>} />
          <ProfileRow label="Email" value={user.email} />
          {user.phone && <ProfileRow label="Phone" value={user.phone} />}
          <ProfileRow
            label="Status"
            value={
              <Badge color={STATUS_COLOR[user.status] ?? "light"} size="sm">
                {user.status}
              </Badge>
            }
          />
          <ProfileRow
            label="Auth Provider"
            value={
              <Badge color={(AUTH_PROVIDER_COLOR[user.authProvider] as "primary" | "light") ?? "light"} size="sm">
                {AUTH_PROVIDER_LABEL[user.authProvider] ?? user.authProvider}
              </Badge>
            }
          />
          <ProfileRow label="Member Since" value={formatDateTime(user.createdAt)} />
          <ProfileRow
            label="Last Login"
            value={<span className="font-mono text-xs">{formatDateTime(user.lastLoginAt)}</span>}
          />
          <ProfileRow label="Groups" value={<span className="font-medium">{groups.length}</span>} />
          <ProfileRow label="Roles" value={<span className="font-medium">{uniqueRoles.length}</span>} />
        </dl>
      </div>

      {/* Group memberships */}
      <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] lg:col-span-2">
        <div className="flex items-center justify-between border-b border-gray-100 dark:border-gray-800 px-5 py-4">
          <div>
            <h3 className="text-sm font-semibold text-gray-800 dark:text-white/90">
              Group memberships
            </h3>
            <p className="mt-0.5 text-xs text-gray-500 dark:text-gray-400">
              {groups.length} group{groups.length !== 1 ? "s" : ""}
            </p>
          </div>
          {!isAdUser && (
            <button
              onClick={onAssignGroup}
              className="text-xs text-brand-500 hover:text-brand-600 font-medium"
            >
              + Add
            </button>
          )}
        </div>

        {groupsLoading ? (
          <div className="flex items-center justify-center py-10 text-sm text-gray-400">
            Loading…
          </div>
        ) : groupsError ? (
          <div className="px-5 py-4 text-sm text-error-600 dark:text-error-400">
            {groupsError}
          </div>
        ) : groups.length === 0 ? (
          <div className="flex flex-col items-center py-10 text-gray-400">
            <GroupIcon className="size-8 mb-2 text-gray-300 dark:text-gray-700" />
            <p className="text-sm">No group memberships</p>
          </div>
        ) : (
          <div className="divide-y divide-gray-100 dark:divide-gray-800">
            {groups.map((g) => (
              <div
                key={g.id}
                className="flex items-center gap-3 px-5 py-3 hover:bg-gray-50 dark:hover:bg-white/[0.02] transition-colors"
              >
                <Badge color="light" size="sm">{g.type}</Badge>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-gray-800 dark:text-white/90">{g.name}</p>
                  <p className="text-xs text-gray-500 dark:text-gray-400 truncate">{g.description}</p>
                </div>
                <span className="text-xs text-gray-400 shrink-0">
                  {g.roles.length} role{g.roles.length !== 1 ? "s" : ""}
                </span>
                {!isAdUser && (
                  <button
                    onClick={() => onRemoveGroup(g.id)}
                    className="ml-1 text-xs text-gray-400 hover:text-error-500 dark:hover:text-error-400 transition-colors"
                    title="Remove from group"
                  >
                    ✕
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Effective roles summary */}
      {uniqueRoles.length > 0 && (
        <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] lg:col-span-3">
          <div className="border-b border-gray-100 dark:border-gray-800 px-5 py-4">
            <h3 className="text-sm font-semibold text-gray-800 dark:text-white/90">
              Effective roles
            </h3>
            <p className="mt-0.5 text-xs text-gray-500 dark:text-gray-400">
              Inherited via group memberships
            </p>
          </div>
          <div className="flex flex-wrap gap-2 p-5">
            {uniqueRoles.map((r) => (
              <div
                key={r.id}
                className="flex items-center gap-2 rounded-lg border border-gray-200 dark:border-gray-700 px-3 py-2"
              >
                <LockIcon className="size-3.5 text-warning-500 shrink-0" />
                <div>
                  <p className="text-xs font-medium text-gray-800 dark:text-white/90">{r.name}</p>
                  <p className="text-xs text-gray-400">via {r.sourceGroup}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

export default memo(UserOverviewTab);
