import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router";
import { useAuth } from "../../context/AuthContext";
import PageMeta from "../../components/common/PageMeta";
import Badge from "../../components/ui/badge/Badge";
import { BoltIcon, ChevronLeftIcon, DocsIcon, GroupIcon, LockIcon } from "../../icons";
import {
  deleteUser,
  getUserById,
  getUserEffectivePermissions,
  getUserGroups,
  listGroups,
  removeUserFromGroup,
  updateUserStatus,
} from "../../services/adminService";
import type { UserGroupDto } from "../../types/admin";
import type { UserDto } from "../../types/auth";
import AssignGroupModal from "./AssignGroupModal";
import EditUserModal from "./EditUserModal";
import UserGroupsTab from "./UserGroupsTab";
import UserOverviewTab from "./UserOverviewTab";
import UserPermissionsTab from "./UserPermissionsTab";
import UserRolesTab from "./UserRolesTab";
import {
  AUTH_PROVIDER_COLOR,
  AUTH_PROVIDER_LABEL,
  STATUS_COLOR,
  getInitials,
  type RoleWithSource,
} from "./userUtils";

type TabId = "overview" | "groups" | "roles" | "permissions" | "activity";

interface TabConfig {
  id: TabId;
  label: string;
  icon: React.ReactNode;
  count?: number;
}

// Skeleton for the loading state of the detail page header
function UserSkeleton() {
  return (
    <div className="animate-pulse">
      <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] mb-1">
        <div className="flex items-start gap-4 px-6 py-5">
          <div className="h-14 w-14 shrink-0 rounded-2xl bg-gray-200 dark:bg-gray-700" />
          <div className="flex-1 space-y-2 pt-1">
            <div className="h-5 w-48 rounded bg-gray-200 dark:bg-gray-700" />
            <div className="h-3.5 w-72 rounded bg-gray-100 dark:bg-gray-800" />
          </div>
        </div>
        <div className="flex gap-6 border-t border-gray-100 dark:border-gray-800 px-6 py-3">
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="h-4 w-20 rounded bg-gray-100 dark:bg-gray-800" />
          ))}
        </div>
      </div>
    </div>
  );
}

export default function UserDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user: currentUser } = useAuth();

  const userId = Number(id);
  const isSelf = currentUser?.id === userId;

  const [user, setUser] = useState<UserDto | null>(null);
  const [userLoading, setUserLoading] = useState(true);
  const [userError, setUserError] = useState<string | null>(null);

  const [groups, setGroups] = useState<UserGroupDto[]>([]);
  const [permissions, setPermissions] = useState<string[]>([]);
  const [groupsLoading, setGroupsLoading] = useState(true);
  const [permsLoading, setPermsLoading] = useState(false);
  const [groupsError, setGroupsError] = useState<string | null>(null);
  const [permsError, setPermsError] = useState<string | null>(null);

  const [activeTab, setActiveTab] = useState<TabId>("overview");
  const [assignOpen, setAssignOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [actionBusy, setActionBusy] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [deleteConfirm, setDeleteConfirm] = useState(false);

  useEffect(() => {
    if (!userId) return;
    setUserLoading(true);
    setUserError(null);
    getUserById(userId)
      .then((res) => setUser(res.data.data ?? null))
      .catch(() => setUserError("Failed to load user."))
      .finally(() => setUserLoading(false));
  }, [userId]);

  useEffect(() => {
    if (!userId) return;
    setGroupsLoading(true);
    setGroupsError(null);
    getUserGroups(userId)
      .then((res) => setGroups(res.data.data ?? []))
      .catch(() => setGroupsError("Failed to load group memberships."))
      .finally(() => setGroupsLoading(false));
  }, [userId]);

  useEffect(() => {
    if (activeTab !== "permissions" || !userId) return;
    setPermsLoading(true);
    setPermsError(null);
    getUserEffectivePermissions(userId)
      .then((res) => setPermissions(res.data.data ?? []))
      .catch(() => setPermsError("Failed to load permissions."))
      .finally(() => setPermsLoading(false));
  }, [activeTab, userId]);

  async function handleRemoveGroup(groupId: number) {
    try {
      await removeUserFromGroup(userId, groupId);
      setGroups((prev) => prev.filter((g) => g.id !== groupId));
    } catch { /* could show a toast here */ }
  }

  function refreshGroups() {
    getUserGroups(userId).then((res) => setGroups(res.data.data ?? []));
  }

  async function handleStatusChange(newStatus: "ACTIVE" | "INACTIVE" | "SUSPENDED") {
    setActionBusy(true);
    setActionError(null);
    try {
      const res = await updateUserStatus(userId, { status: newStatus });
      setUser(res.data.data ?? null);
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setActionError(msg ?? "Failed to update status.");
    } finally {
      setActionBusy(false);
    }
  }

  async function handleDelete() {
    setActionBusy(true);
    setActionError(null);
    try {
      await deleteUser(userId);
      navigate("/users");
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setActionError(msg ?? "Failed to delete user.");
      setDeleteConfirm(false);
    } finally {
      setActionBusy(false);
    }
  }

  if (userLoading) {
    return (
      <>
        <button
          onClick={() => navigate("/users")}
          className="mb-4 inline-flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200 transition-colors"
        >
          <ChevronLeftIcon className="size-4" />
          Users
        </button>
        <UserSkeleton />
      </>
    );
  }

  if (userError || !user) {
    return (
      <div className="flex flex-col items-center justify-center py-24 text-gray-400">
        <GroupIcon className="size-12 mb-3 text-gray-300 dark:text-gray-700" />
        <p className="text-base font-medium">{userError ?? "User not found"}</p>
        <button
          onClick={() => navigate("/users")}
          className="mt-4 text-sm text-brand-500 hover:text-brand-600"
        >
          Back to Users
        </button>
      </div>
    );
  }

  const isAdUser = user.authProvider === "AZURE_AD";

  const allRoles = groups.flatMap((g) =>
    g.roles.map((r) => ({ ...r, sourceGroup: g.name }))
  );
  const uniqueRoles: RoleWithSource[] = allRoles.filter(
    (r, idx, arr) => arr.findIndex((x) => x.id === r.id) === idx
  );

  const currentGroupIds = groups.map((g) => g.id);

  const tabs: TabConfig[] = [
    { id: "overview",    label: "Overview",    icon: <GroupIcon className="size-4" /> },
    { id: "groups",      label: "Groups",      icon: <GroupIcon className="size-4" />, count: groups.length },
    { id: "roles",       label: "Roles",       icon: <LockIcon className="size-4" />, count: uniqueRoles.length },
    { id: "permissions", label: "Permissions", icon: <BoltIcon className="size-4" />, count: permissions.length || undefined },
    { id: "activity",    label: "Activity",    icon: <DocsIcon className="size-4" /> },
  ];

  return (
    <>
      <PageMeta
        title={`${user.name} | Users | Auth Platform`}
        description={`User profile and access control for ${user.name}`}
      />

      {/* Back nav */}
      <button
        onClick={() => navigate("/users")}
        className="mb-4 inline-flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200 transition-colors"
      >
        <ChevronLeftIcon className="size-4" />
        Users
      </button>

      {/* Profile header */}
      <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] mb-1">
        <div className="flex flex-col gap-4 px-6 py-5 sm:flex-row sm:items-start">
          {/* Avatar */}
          <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-brand-50 dark:bg-brand-500/15 text-brand-600 dark:text-brand-400 text-xl font-bold">
            {getInitials(user.name)}
          </div>

          {/* Info */}
          <div className="flex-1 min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <h1 className="text-xl font-semibold text-gray-800 dark:text-white/90">
                {user.name}
              </h1>
              <Badge color={STATUS_COLOR[user.status] ?? "light"} size="sm">
                {user.status}
              </Badge>
              <Badge
                color={(AUTH_PROVIDER_COLOR[user.authProvider] as "primary" | "light") ?? "light"}
                size="sm"
              >
                {AUTH_PROVIDER_LABEL[user.authProvider] ?? user.authProvider}
              </Badge>
            </div>
            <div className="mt-1.5 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-gray-500 dark:text-gray-400">
              <span className="font-mono text-xs">#{user.id}</span>
              <span>{user.email}</span>
              {user.phone && <span>{user.phone}</span>}
            </div>
          </div>

          {/* Actions */}
          <div className="flex flex-wrap items-center gap-2 shrink-0">
            {isSelf && (
              <span className="rounded-lg border border-brand-200 bg-brand-50 px-3 py-2 text-xs font-medium text-brand-600 dark:border-brand-500/30 dark:bg-brand-500/10 dark:text-brand-400">
                Your account
              </span>
            )}

            {/* Edit */}
            {isAdUser ? (
              <span
                title="Profile is managed by Azure AD — changes must be made in Active Directory"
                className="inline-flex cursor-not-allowed items-center gap-1.5 rounded-lg border border-gray-200 px-3 py-2 text-sm font-medium text-gray-400 dark:border-gray-700 dark:text-gray-600"
              >
                Edit
                <span className="rounded bg-warning-100 px-1 py-0.5 text-xs text-warning-700 dark:bg-warning-500/15 dark:text-warning-400">
                  AD
                </span>
              </span>
            ) : (
              <button
                onClick={() => setEditOpen(true)}
                className="rounded-lg border border-gray-200 px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-white/5 transition-colors"
              >
                Edit
              </button>
            )}

            {/* Suspend / Activate */}
            {!isSelf && user.status !== "DELETED" && user.status !== "NEW" && (
              user.status === "ACTIVE" ? (
                <button
                  disabled={actionBusy}
                  onClick={() => handleStatusChange("SUSPENDED")}
                  className="rounded-lg border border-error-200 px-3 py-2 text-sm font-medium text-error-600 hover:bg-error-50 disabled:opacity-50 disabled:cursor-not-allowed dark:border-error-500/30 dark:text-error-400 dark:hover:bg-error-500/10 transition-colors"
                >
                  {actionBusy ? "…" : "Suspend"}
                </button>
              ) : (
                <button
                  disabled={actionBusy}
                  onClick={() => handleStatusChange("ACTIVE")}
                  className="rounded-lg border border-success-200 px-3 py-2 text-sm font-medium text-success-600 hover:bg-success-50 disabled:opacity-50 disabled:cursor-not-allowed dark:border-success-500/30 dark:text-success-400 dark:hover:bg-success-500/10 transition-colors"
                >
                  {actionBusy ? "…" : "Activate"}
                </button>
              )
            )}

            {/* Assign group */}
            {isAdUser ? (
              <span
                title="Group memberships are managed in Active Directory — changes must be made in AD/Keycloak"
                className="inline-flex cursor-not-allowed items-center gap-1.5 rounded-lg border border-gray-200 px-3 py-2 text-sm font-medium text-gray-400 dark:border-gray-700 dark:text-gray-600"
              >
                + Assign group
                <span className="rounded bg-warning-100 px-1 py-0.5 text-xs text-warning-700 dark:bg-warning-500/15 dark:text-warning-400">
                  AD
                </span>
              </span>
            ) : (
              <button
                onClick={() => setAssignOpen(true)}
                className="rounded-lg bg-brand-500 px-3 py-2 text-sm font-medium text-white hover:bg-brand-600 transition-colors"
              >
                + Assign group
              </button>
            )}

            {/* Delete */}
            {!isSelf && user.status !== "DELETED" && (
              deleteConfirm ? (
                <div className="flex items-center gap-1.5">
                  <span className="text-xs text-gray-500 dark:text-gray-400">Confirm delete?</span>
                  <button
                    disabled={actionBusy}
                    onClick={handleDelete}
                    className="rounded-lg bg-error-500 px-3 py-2 text-sm font-medium text-white hover:bg-error-600 disabled:opacity-50 transition-colors"
                  >
                    {actionBusy ? "Deleting…" : "Yes, delete"}
                  </button>
                  <button
                    onClick={() => setDeleteConfirm(false)}
                    className="rounded-lg border border-gray-200 px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-white/5"
                  >
                    Cancel
                  </button>
                </div>
              ) : (
                <button
                  onClick={() => setDeleteConfirm(true)}
                  className="rounded-lg border border-error-200 px-3 py-2 text-sm font-medium text-error-600 hover:bg-error-50 dark:border-error-500/30 dark:text-error-400 dark:hover:bg-error-500/10 transition-colors"
                >
                  Delete
                </button>
              )
            )}
          </div>
        </div>

        {/* Tabs */}
        <div className="flex overflow-x-auto border-t border-gray-100 dark:border-gray-800 px-6">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`flex items-center gap-1.5 border-b-2 px-3 py-3.5 text-sm font-medium whitespace-nowrap transition-colors ${
                activeTab === tab.id
                  ? "border-brand-500 text-brand-600 dark:text-brand-400"
                  : "border-transparent text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200"
              }`}
            >
              {tab.label}
              {tab.count !== undefined && (
                <span
                  className={`rounded-full px-1.5 py-0.5 text-xs tabular-nums ${
                    activeTab === tab.id
                      ? "bg-brand-50 text-brand-600 dark:bg-brand-500/15 dark:text-brand-400"
                      : "bg-gray-100 text-gray-500 dark:bg-gray-800 dark:text-gray-400"
                  }`}
                >
                  {tab.count}
                </span>
              )}
            </button>
          ))}
        </div>
      </div>

      {/* Action error banner */}
      {actionError && (
        <div className="mt-3 flex items-center justify-between rounded-xl border border-error-200 bg-error-50 px-5 py-2 text-sm text-error-700 dark:border-error-500/20 dark:bg-error-500/10 dark:text-error-400">
          {actionError}
          <button
            onClick={() => setActionError(null)}
            className="ml-4 font-medium underline hover:no-underline"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Tab content */}
      <div className="mt-4 space-y-4">
        {activeTab === "overview" && (
          <UserOverviewTab
            user={user}
            groups={groups}
            uniqueRoles={uniqueRoles}
            groupsLoading={groupsLoading}
            groupsError={groupsError}
            isAdUser={isAdUser}
            onAssignGroup={() => setAssignOpen(true)}
            onRemoveGroup={handleRemoveGroup}
          />
        )}

        {activeTab === "groups" && (
          <UserGroupsTab
            groups={groups}
            groupsLoading={groupsLoading}
            isAdUser={isAdUser}
            onAssignGroup={() => setAssignOpen(true)}
            onRemoveGroup={handleRemoveGroup}
          />
        )}

        {activeTab === "roles" && <UserRolesTab uniqueRoles={uniqueRoles} />}

        {activeTab === "permissions" && (
          <UserPermissionsTab
            permissions={permissions}
            loading={permsLoading}
            error={permsError}
          />
        )}

        {activeTab === "activity" && (
          <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] overflow-hidden">
            <div className="border-b border-gray-100 dark:border-gray-800 px-6 py-4">
              <h3 className="text-sm font-semibold text-gray-800 dark:text-white/90">
                Audit activity
              </h3>
              <p className="mt-0.5 text-xs text-gray-500 dark:text-gray-400">
                Actions performed by or on this user
              </p>
            </div>
            <div className="flex flex-col items-center py-12 text-gray-400">
              <DocsIcon className="size-10 mb-2 text-gray-300 dark:text-gray-700" />
              <p className="text-sm">Audit log coming soon</p>
            </div>
          </div>
        )}
      </div>

      {assignOpen && !isAdUser && (
        <AssignGroupModal
          userId={userId}
          currentGroupIds={currentGroupIds}
          onClose={() => setAssignOpen(false)}
          onAssigned={refreshGroups}
        />
      )}

      {editOpen && !isAdUser && (
        <EditUserModal
          user={user}
          onClose={() => setEditOpen(false)}
          onSaved={(updated) => setUser(updated)}
        />
      )}
    </>
  );
}
