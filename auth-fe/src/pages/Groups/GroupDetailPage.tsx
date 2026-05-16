import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router";
import PageMeta from "../../components/common/PageMeta";
import Badge from "../../components/ui/badge/Badge";
import { ChevronLeftIcon, GroupIcon, UserCircleIcon } from "../../icons";
import { getGroup, removeRoleFromGroup } from "../../services/adminService";
import type { PermissionDto, UserGroupDto } from "../../types/admin";
import AssignRoleModal from "./AssignRoleModal";
import EditGroupModal from "./EditGroupModal";
import GroupOverviewTab from "./GroupOverviewTab";
import GroupPermissionsTab from "./GroupPermissionsTab";
import GroupRolesTab from "./GroupRolesTab";
import { typeColor } from "./groupUtils";

type TabId = "overview" | "roles" | "permissions" | "members";

export default function GroupDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [group, setGroup] = useState<UserGroupDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<TabId>("overview");
  const [assignOpen, setAssignOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);

  const groupId = Number(id);

  const fetchGroup = useCallback(() => {
    setLoading(true);
    setError(null);
    getGroup(groupId)
      .then((res) => setGroup(res.data.data ?? null))
      .catch(() => setError("Failed to load group."))
      .finally(() => setLoading(false));
  }, [groupId]);

  useEffect(() => {
    if (groupId) fetchGroup();
  }, [groupId, fetchGroup]);

  async function handleRemoveRole(roleId: number) {
    try {
      await removeRoleFromGroup(groupId, roleId);
      setGroup((prev) =>
        prev
          ? { ...prev, roles: prev.roles.filter((r) => r.id !== roleId) }
          : prev
      );
    } catch {
      // could show a toast
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-24 text-sm text-gray-400">
        Loading…
      </div>
    );
  }

  if (error || !group) {
    return (
      <div className="flex flex-col items-center justify-center py-24 text-gray-400">
        <GroupIcon className="size-12 mb-3 text-gray-300 dark:text-gray-700" />
        <p className="text-base font-medium">{error ?? "Group not found"}</p>
        <button
          onClick={() => navigate("/groups")}
          className="mt-4 text-sm text-brand-500 hover:text-brand-600"
        >
          Back to Groups
        </button>
      </div>
    );
  }

  // Derived data
  const allPermissions: PermissionDto[] = [
    ...new Map(
      group.roles.flatMap((r) => r.permissions).map((p) => [p.id, p])
    ).values(),
  ].sort((a, b) => a.code.localeCompare(b.code));

  const permsByCategory = allPermissions.reduce<
    Record<string, PermissionDto[]>
  >((acc, p) => {
    if (!acc[p.category]) acc[p.category] = [];
    acc[p.category].push(p);
    return acc;
  }, {});

  const tabs: { id: TabId; label: string; count?: number }[] = [
    { id: "overview", label: "Overview" },
    { id: "roles", label: "Roles", count: group.roles.length },
    { id: "permissions", label: "Permissions", count: allPermissions.length },
    { id: "members", label: "Members" },
  ];

  return (
    <>
      <PageMeta
        title={`${group.name} | Groups | Auth Platform`}
        description={`Group detail and role assignments for ${group.name}`}
      />

      <button
        onClick={() => navigate("/groups")}
        className="mb-4 inline-flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200 transition-colors"
      >
        <ChevronLeftIcon className="size-4" /> Groups
      </button>

      {/* Group header */}
      <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] mb-1">
        <div className="flex flex-col gap-4 px-6 py-5 sm:flex-row sm:items-start">
          <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800">
            <GroupIcon className="size-6 text-gray-500 dark:text-gray-400" />
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <h1 className="text-xl font-semibold text-gray-800 dark:text-white/90">
                {group.name}
              </h1>
              <Badge color={typeColor(group.type)} size="sm">
                {group.type}
              </Badge>
            </div>
            <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
              {group.description}
            </p>
            <div className="mt-2 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-gray-400 dark:text-gray-500">
              <span>
                {group.roles.length} role{group.roles.length === 1 ? "" : "s"}
              </span>
              <span>&middot;</span>
              <span>{allPermissions.length} effective permissions</span>
            </div>
          </div>
          <div className="flex flex-wrap gap-2 shrink-0">
            <button
              onClick={() => setEditOpen(true)}
              className="rounded-lg border border-gray-200 px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-white/5 transition-colors"
            >
              Edit
            </button>
            <button
              onClick={() => setAssignOpen(true)}
              className="rounded-lg bg-brand-500 px-3 py-2 text-sm font-medium text-white hover:bg-brand-600 transition-colors"
            >
              + Assign role
            </button>
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

      {/* Tab content */}
      <div className="mt-4 space-y-4">
        {activeTab === "overview" && (
          <GroupOverviewTab
            roles={group.roles}
            allPermissions={allPermissions}
            permsByCategory={permsByCategory}
            onAssignRole={() => setAssignOpen(true)}
            onRemoveRole={handleRemoveRole}
          />
        )}
        {activeTab === "roles" && (
          <GroupRolesTab
            roles={group.roles}
            onAssignRole={() => setAssignOpen(true)}
            onRemoveRole={handleRemoveRole}
          />
        )}
        {activeTab === "permissions" && (
          <GroupPermissionsTab allPermissions={allPermissions} />
        )}
        {activeTab === "members" && (
          <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] overflow-hidden">
            <div className="border-b border-gray-100 dark:border-gray-800 px-6 py-4">
              <h3 className="text-sm font-semibold text-gray-800 dark:text-white/90">
                Members
              </h3>
              <p className="mt-0.5 text-xs text-gray-500 dark:text-gray-400">
                Users assigned to this group
              </p>
            </div>
            <div className="flex flex-col items-center py-14 text-gray-400 dark:text-gray-500">
              <UserCircleIcon className="size-10 mb-3 text-gray-300 dark:text-gray-700" />
              <p className="text-sm font-medium">Members list coming soon</p>
              <p className="mt-1 text-xs text-center max-w-xs">
                View and manage group members from the{" "}
                <button
                  onClick={() => navigate("/users")}
                  className="text-brand-500 hover:text-brand-600 underline"
                >
                  Users page.
                </button>
              </p>
            </div>
          </div>
        )}
      </div>

      {assignOpen && (
        <AssignRoleModal
          groupId={groupId}
          currentRoleIds={group.roles.map((r) => r.id)}
          onClose={() => setAssignOpen(false)}
          onAssigned={fetchGroup}
        />
      )}
      {editOpen && (
        <EditGroupModal
          group={group}
          onClose={() => setEditOpen(false)}
          onSaved={(updated) => setGroup(updated)}
        />
      )}
    </>
  );
}
