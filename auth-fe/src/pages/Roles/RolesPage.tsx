import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router";
import PageMeta from "../../components/common/PageMeta";
import Badge from "../../components/ui/badge/Badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHeader,
  TableRow,
} from "../../components/ui/table";
import { LockIcon, GroupIcon } from "../../icons";
import { listRoles } from "../../services/adminService";
import type { RoleDto } from "../../types/admin";
import { GROUPS, USERS } from "../../temp_data/rbacData";

export default function RolesPage() {
  const navigate = useNavigate();

  const [roles, setRoles] = useState<RoleDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listRoles()
      .then((res) => setRoles(res.data.data ?? []))
      .catch(() => setError("Failed to load roles."))
      .finally(() => setLoading(false));
  }, []);

  const enriched = useMemo(
    () =>
      roles.map((r) => ({
        ...r,
        groupCount: GROUPS.filter((g) => g.roles.some((gr) => gr === r.name)).length,
        userCount: USERS.filter((u) =>
          u.groups.some((gn) =>
            GROUPS.find((g) => g.name === gn)?.roles.some((gr) => gr === r.name)
          )
        ).length,
      })),
    [roles]
  );

  const totalBindings = roles.reduce((sum, r) => sum + r.permissions.length, 0);

  return (
    <>
      <PageMeta
        title="Roles | Auth Platform"
        description="Manage banking roles and their permission assignments"
      />

      {/* ── Page header ── */}
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-800 dark:text-white/90">
            Roles
          </h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            {roles.length} roles &middot; {totalBindings} role-permission bindings
          </p>
        </div>
        <button className="inline-flex items-center gap-2 rounded-lg bg-brand-500 px-4 py-2 text-sm font-medium text-white hover:bg-brand-600 transition-colors">
          <LockIcon className="size-4" />
          New role
        </button>
      </div>

      {/* ── Table ── */}
      <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center py-16 text-sm text-gray-400">
            Loading roles…
          </div>
        ) : error ? (
          <div className="flex items-center justify-center py-16 text-sm text-error-600 dark:text-error-400">
            {error}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow className="border-b border-gray-100 dark:border-gray-800">
                  {[
                    { label: "Role", className: "" },
                    { label: "Permissions", className: "w-32 text-right" },
                    { label: "Groups", className: "w-24 text-right hidden md:table-cell" },
                    { label: "Users", className: "w-24 text-right hidden lg:table-cell" },
                  ].map(({ label, className }) => (
                    <TableCell
                      key={label}
                      isHeader
                      className={`px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500 dark:text-gray-400 ${className}`}
                    >
                      {label}
                    </TableCell>
                  ))}
                </TableRow>
              </TableHeader>
              <TableBody className="divide-y divide-gray-100 dark:divide-gray-800">
                {enriched.length === 0 ? (
                  <TableRow>
                    <TableCell className="px-6 py-12 text-center text-sm text-gray-400">
                      <div className="flex flex-col items-center gap-2">
                        <LockIcon className="size-8 text-gray-300 dark:text-gray-600" />
                        No roles found
                      </div>
                    </TableCell>
                  </TableRow>
                ) : (
                  enriched.map((r) => (
                    <TableRow
                      key={r.id}
                      className="hover:bg-gray-50 dark:hover:bg-white/[0.02] cursor-pointer transition-colors"
                      onClick={() => navigate(`/roles/${r.id}`)}
                    >
                      {/* Role name + description */}
                      <TableCell className="px-6 py-3">
                        <div className="flex items-center gap-3">
                          <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl border border-warning-200 dark:border-warning-500/30 bg-warning-50 dark:bg-warning-500/10">
                            <LockIcon className="size-4 text-warning-500" />
                          </div>
                          <div className="min-w-0">
                            <p className="text-sm font-medium font-mono text-gray-800 dark:text-white/90">
                              {r.name}
                            </p>
                            <p className="text-xs text-gray-500 dark:text-gray-400 truncate max-w-sm">
                              {r.description}
                            </p>
                          </div>
                        </div>
                      </TableCell>

                      {/* Permissions count */}
                      <TableCell className="px-6 py-3 text-right">
                        <span className="text-sm font-semibold text-gray-800 dark:text-white/90 tabular-nums">
                          {r.permissions.length}
                        </span>
                      </TableCell>

                      {/* Groups */}
                      <TableCell className="px-6 py-3 text-right text-sm text-gray-600 dark:text-gray-400 tabular-nums hidden md:table-cell">
                        {r.groupCount}
                      </TableCell>

                      {/* Users */}
                      <TableCell className="px-6 py-3 text-right text-sm text-gray-600 dark:text-gray-400 tabular-nums hidden lg:table-cell">
                        {r.userCount}
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </div>
        )}
      </div>
    </>
  );
}
