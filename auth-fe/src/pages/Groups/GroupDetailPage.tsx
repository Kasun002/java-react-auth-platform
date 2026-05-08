import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router";
import PageMeta from "../../components/common/PageMeta";
import Badge from "../../components/ui/badge/Badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHeader,
  TableRow,
} from "../../components/ui/table";
import {
  ChevronLeftIcon,
  GroupIcon,
  LockIcon,
  BoltIcon,
  UserCircleIcon,
} from "../../icons";
import {
  getGroup,
  listRoles,
  assignRoleToGroup,
  removeRoleFromGroup,
} from "../../services/adminService";
import type { BankingRoleDto, UserGroupDto } from "../../types/admin";
import { USERS } from "../../temp_data/rbacData";

// ── Helpers ───────────────────────────────────────────────────────────────────

const TYPE_COLOR: Record<string, "primary" | "success" | "warning" | "error" | "light"> = {
  CUSTOMER: "primary",
  STAFF: "success",
  OVERSIGHT: "warning",
  ADMIN: "error",
};

const STATUS_COLOR: Record<string, "success" | "error" | "warning" | "light"> = {
  ACTIVE: "success",
  INACTIVE: "light",
  SUSPENDED: "error",
};

function getInitials(name: string) {
  return name.split(" ").map((n) => n[0]).slice(0, 2).join("").toUpperCase();
}

function formatDate(iso: string | null) {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString(undefined, {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
}

type TabId = "overview" | "roles" | "permissions" | "members";

// ── Assign Role Modal ─────────────────────────────────────────────────────────

interface AssignRoleModalProps {
  groupId: number;
  currentRoleIds: number[];
  onClose: () => void;
  onAssigned: () => void;
}

function AssignRoleModal({
  groupId,
  currentRoleIds,
  onClose,
  onAssigned,
}: AssignRoleModalProps) {
  const [availableRoles, setAvailableRoles] = useState<BankingRoleDto[]>([]);
  const [selected, setSelected] = useState<number | null>(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listRoles()
      .then((res) => {
        const all = res.data.data ?? [];
        setAvailableRoles(all.filter((r) => !currentRoleIds.includes(r.id)));
      })
      .catch(() => setError("Failed to load roles."));
  }, [currentRoleIds]);

  async function handleAssign() {
    if (!selected) return;
    setSaving(true);
    setError(null);
    try {
      await assignRoleToGroup(groupId, selected);
      onAssigned();
      onClose();
    } catch {
      setError("Failed to assign role. Please try again.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="w-full max-w-lg rounded-2xl border border-gray-200 bg-white shadow-xl dark:border-gray-800 dark:bg-gray-900">
        <div className="flex items-center justify-between border-b border-gray-100 dark:border-gray-800 px-6 py-4">
          <h3 className="text-base font-semibold text-gray-800 dark:text-white/90">
            Assign role to group
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
            Assigning a role grants all users in this group its permissions. Effective permissions update on next login.
          </p>

          {error && (
            <div className="mb-3 rounded-lg border border-error-200 bg-error-50 px-4 py-2 text-sm text-error-700 dark:bg-error-500/10 dark:border-error-500/20 dark:text-error-400">
              {error}
            </div>
          )}

          <div className="max-h-72 overflow-y-auto rounded-xl border border-gray-200 dark:border-gray-700">
            {availableRoles.length === 0 ? (
              <p className="px-4 py-6 text-center text-sm text-gray-400">
                No available roles
              </p>
            ) : (
              availableRoles.map((r) => (
                <label
                  key={r.id}
                  className={`flex cursor-pointer items-center gap-3 border-b border-gray-100 dark:border-gray-800 last:border-0 px-4 py-3 hover:bg-gray-50 dark:hover:bg-white/[0.02] transition-colors ${
                    selected === r.id ? "bg-brand-50 dark:bg-brand-500/10" : ""
                  }`}
                >
                  <input
                    type="radio"
                    name="role-select"
                    checked={selected === r.id}
                    onChange={() => setSelected(r.id)}
                    className="accent-brand-500"
                  />
                  <LockIcon className="size-4 text-warning-500 shrink-0" />
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium font-mono text-gray-800 dark:text-white/90">
                      {r.name}
                    </p>
                    <p className="truncate text-xs text-gray-500 dark:text-gray-400">
                      {r.description}
                    </p>
                  </div>
                  <span className="shrink-0 text-xs text-gray-400">
                    {r.permissions.length} perm{r.permissions.length !== 1 ? "s" : ""}
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
            {saving ? "Assigning…" : "Assign role"}
          </button>
        </div>
      </div>
    </div>
  );
}

// ── Main Component ────────────────────────────────────────────────────────────

export default function GroupDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [group, setGroup] = useState<UserGroupDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<TabId>("overview");
  const [assignOpen, setAssignOpen] = useState(false);

  const groupId = Number(id);

  function fetchGroup() {
    setLoading(true);
    setError(null);
    getGroup(groupId)
      .then((res) => setGroup(res.data.data ?? null))
      .catch(() => setError("Failed to load group."))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    if (!groupId) return;
    fetchGroup();
  }, [groupId]);

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
  const allPermissions = [
    ...new Map(
      group.roles
        .flatMap((r) => r.permissions)
        .map((p) => [p.id, p])
    ).values(),
  ].sort((a, b) => a.code.localeCompare(b.code));

  const permsByCategory = allPermissions.reduce<Record<string, typeof allPermissions>>(
    (acc, p) => {
      if (!acc[p.category]) acc[p.category] = [];
      acc[p.category].push(p);
      return acc;
    },
    {}
  );

  // Members from temp data matched by group name
  const members = USERS.filter((u) => u.groups.includes(group.name));

  const currentRoleIds = group.roles.map((r) => r.id);

  const tabs: { id: TabId; label: string; count?: number }[] = [
    { id: "overview", label: "Overview" },
    { id: "roles", label: "Roles", count: group.roles.length },
    { id: "permissions", label: "Permissions", count: allPermissions.length },
    { id: "members", label: "Members", count: members.length },
  ];

  return (
    <>
      <PageMeta
        title={`${group.name} | Groups | Auth Platform`}
        description={`Group detail and role assignments for ${group.name}`}
      />

      {/* ── Back nav ── */}
      <button
        onClick={() => navigate("/groups")}
        className="mb-4 inline-flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200 transition-colors"
      >
        <ChevronLeftIcon className="size-4" />
        Groups
      </button>

      {/* ── Group header ── */}
      <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] mb-1">
        <div className="flex flex-col gap-4 px-6 py-5 sm:flex-row sm:items-start">
          {/* Icon */}
          <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800">
            <GroupIcon className="size-6 text-gray-500 dark:text-gray-400" />
          </div>

          {/* Info */}
          <div className="flex-1 min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <h1 className="text-xl font-semibold text-gray-800 dark:text-white/90">
                {group.name}
              </h1>
              <Badge color={TYPE_COLOR[group.type] ?? "light"} size="sm">
                {group.type}
              </Badge>
            </div>
            <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
              {group.description}
            </p>
            <div className="mt-2 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-gray-400 dark:text-gray-500">
              <span>{members.length} member{members.length !== 1 ? "s" : ""}</span>
              <span>&middot;</span>
              <span>{group.roles.length} role{group.roles.length !== 1 ? "s" : ""}</span>
              <span>&middot;</span>
              <span>{allPermissions.length} effective permissions</span>
            </div>
          </div>

          {/* Actions */}
          <div className="flex flex-wrap gap-2 shrink-0">
            <button className="rounded-lg border border-gray-200 px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-white/5 transition-colors">
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

      {/* ── Tab content ── */}
      <div className="mt-4 space-y-4">

        {/* Overview */}
        {activeTab === "overview" && (
          <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
            {/* Assigned roles */}
            <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] overflow-hidden">
              <div className="flex items-center justify-between border-b border-gray-100 dark:border-gray-800 px-5 py-4">
                <h3 className="text-sm font-semibold text-gray-800 dark:text-white/90">
                  Assigned roles
                </h3>
                <button
                  onClick={() => setAssignOpen(true)}
                  className="text-xs text-brand-500 hover:text-brand-600 font-medium"
                >
                  + Add
                </button>
              </div>
              {group.roles.length === 0 ? (
                <div className="flex flex-col items-center py-10 text-gray-400">
                  <LockIcon className="size-8 mb-2 text-gray-300 dark:text-gray-700" />
                  <p className="text-sm">No roles assigned</p>
                </div>
              ) : (
                <div className="divide-y divide-gray-100 dark:divide-gray-800">
                  {group.roles.map((r) => (
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
                        {r.permissions.length} perm{r.permissions.length !== 1 ? "s" : ""}
                      </span>
                      <button
                        onClick={() => handleRemoveRole(r.id)}
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
                  {allPermissions.length} permissions across {Object.keys(permsByCategory).length} categories
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
                        <Badge color="light" size="sm">{category}</Badge>
                        <span className="text-xs tabular-nums text-gray-400">{perms.length}</span>
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
        )}

        {/* Roles tab */}
        {activeTab === "roles" && (
          <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] overflow-hidden">
            <div className="flex items-center justify-between border-b border-gray-100 dark:border-gray-800 px-6 py-4">
              <h3 className="text-sm font-semibold text-gray-800 dark:text-white/90">
                Assigned roles
              </h3>
              <button
                onClick={() => setAssignOpen(true)}
                className="rounded-lg bg-brand-500 px-3 py-1.5 text-xs font-medium text-white hover:bg-brand-600 transition-colors"
              >
                + Assign role
              </button>
            </div>
            {group.roles.length === 0 ? (
              <div className="flex flex-col items-center py-12 text-gray-400">
                <LockIcon className="size-10 mb-2 text-gray-300 dark:text-gray-700" />
                <p className="text-sm">No roles assigned to this group</p>
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow className="border-b border-gray-100 dark:border-gray-800">
                    {["Role", "Description", "Permissions", ""].map((h) => (
                      <TableCell
                        key={h}
                        isHeader
                        className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500 dark:text-gray-400"
                      >
                        {h}
                      </TableCell>
                    ))}
                  </TableRow>
                </TableHeader>
                <TableBody className="divide-y divide-gray-100 dark:divide-gray-800">
                  {group.roles.map((r) => (
                    <TableRow
                      key={r.id}
                      className="hover:bg-gray-50 dark:hover:bg-white/[0.02] transition-colors"
                    >
                      <TableCell className="px-6 py-3">
                        <div className="flex items-center gap-2">
                          <LockIcon className="size-3.5 text-warning-500 shrink-0" />
                          <span className="text-sm font-medium font-mono text-gray-800 dark:text-white/90">
                            {r.name}
                          </span>
                        </div>
                      </TableCell>
                      <TableCell className="px-6 py-3 text-sm text-gray-500 dark:text-gray-400">
                        {r.description}
                      </TableCell>
                      <TableCell className="px-6 py-3 text-sm tabular-nums text-gray-600 dark:text-gray-400">
                        {r.permissions.length}
                      </TableCell>
                      <TableCell className="px-6 py-3">
                        <button
                          onClick={() => handleRemoveRole(r.id)}
                          className="rounded-lg border border-error-200 px-3 py-1 text-xs font-medium text-error-600 hover:bg-error-50 dark:border-error-500/30 dark:text-error-400 dark:hover:bg-error-500/10 transition-colors"
                        >
                          Remove
                        </button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </div>
        )}

        {/* Permissions tab */}
        {activeTab === "permissions" && (
          <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] overflow-hidden">
            <div className="border-b border-gray-100 dark:border-gray-800 px-6 py-4">
              <h3 className="text-sm font-semibold text-gray-800 dark:text-white/90">
                Effective permissions
              </h3>
              <p className="mt-0.5 text-xs text-gray-500 dark:text-gray-400">
                {allPermissions.length} permissions — union of all assigned roles
              </p>
            </div>
            {allPermissions.length === 0 ? (
              <div className="flex flex-col items-center py-12 text-gray-400">
                <BoltIcon className="size-10 mb-2 text-gray-300 dark:text-gray-700" />
                <p className="text-sm">No permissions</p>
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow className="border-b border-gray-100 dark:border-gray-800">
                    {["Permission", "Category", "Description"].map((h) => (
                      <TableCell
                        key={h}
                        isHeader
                        className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500 dark:text-gray-400"
                      >
                        {h}
                      </TableCell>
                    ))}
                  </TableRow>
                </TableHeader>
                <TableBody className="divide-y divide-gray-100 dark:divide-gray-800">
                  {allPermissions.map((p) => (
                    <TableRow
                      key={p.id}
                      className="hover:bg-gray-50 dark:hover:bg-white/[0.02] transition-colors"
                    >
                      <TableCell className="px-6 py-3">
                        <div className="flex items-center gap-2">
                          <BoltIcon className="size-3.5 text-brand-500 shrink-0" />
                          <span className="text-xs font-mono text-gray-700 dark:text-gray-300">
                            {p.code}
                          </span>
                        </div>
                      </TableCell>
                      <TableCell className="px-6 py-3">
                        <Badge color="light" size="sm">{p.category}</Badge>
                      </TableCell>
                      <TableCell className="px-6 py-3 text-sm text-gray-500 dark:text-gray-400">
                        {p.description}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </div>
        )}

        {/* Members tab */}
        {activeTab === "members" && (
          <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] overflow-hidden">
            <div className="border-b border-gray-100 dark:border-gray-800 px-6 py-4">
              <h3 className="text-sm font-semibold text-gray-800 dark:text-white/90">
                Members
              </h3>
              <p className="mt-0.5 text-xs text-gray-500 dark:text-gray-400">
                {members.length} user{members.length !== 1 ? "s" : ""} in this group
              </p>
            </div>
            {members.length === 0 ? (
              <div className="flex flex-col items-center py-12 text-gray-400">
                <UserCircleIcon className="size-10 mb-2 text-gray-300 dark:text-gray-700" />
                <p className="text-sm">No members in this group</p>
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow className="border-b border-gray-100 dark:border-gray-800">
                    {["User", "Status", "Department", "Last Login"].map((h) => (
                      <TableCell
                        key={h}
                        isHeader
                        className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500 dark:text-gray-400"
                      >
                        {h}
                      </TableCell>
                    ))}
                  </TableRow>
                </TableHeader>
                <TableBody className="divide-y divide-gray-100 dark:divide-gray-800">
                  {members.map((u) => (
                    <TableRow
                      key={u.id}
                      className="hover:bg-gray-50 dark:hover:bg-white/[0.02] cursor-pointer transition-colors"
                      onClick={() => navigate(`/users/${u.id}`)}
                    >
                      <TableCell className="px-6 py-3">
                        <div className="flex items-center gap-3">
                          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-brand-50 dark:bg-brand-500/15 text-brand-600 dark:text-brand-400 text-xs font-semibold">
                            {getInitials(u.name)}
                          </div>
                          <div className="min-w-0">
                            <p className="text-sm font-medium text-gray-800 dark:text-white/90 truncate">
                              {u.name}
                            </p>
                            <p className="text-xs text-gray-500 dark:text-gray-400 truncate">
                              {u.email}
                            </p>
                          </div>
                        </div>
                      </TableCell>
                      <TableCell className="px-6 py-3">
                        <Badge color={STATUS_COLOR[u.status] ?? "light"} size="sm">
                          {u.status}
                        </Badge>
                      </TableCell>
                      <TableCell className="px-6 py-3 text-sm text-gray-600 dark:text-gray-400">
                        {u.department}
                      </TableCell>
                      <TableCell className="px-6 py-3 text-xs font-mono text-gray-500 dark:text-gray-400">
                        {formatDate(u.lastLoginAt)}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </div>
        )}
      </div>

      {/* Assign role modal */}
      {assignOpen && (
        <AssignRoleModal
          groupId={groupId}
          currentRoleIds={currentRoleIds}
          onClose={() => setAssignOpen(false)}
          onAssigned={fetchGroup}
        />
      )}
    </>
  );
}
