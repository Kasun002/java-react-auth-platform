import { useEffect, useState } from "react";
import { useNavigate } from "react-router";
import PageMeta from "../../components/common/PageMeta";
import {
  Table,
  TableBody,
  TableCell,
  TableHeader,
  TableRow,
} from "../../components/ui/table";
import { LockIcon } from "../../icons";
import { deleteRole, listRoles } from "../../services/adminService";
import type { RoleDto } from "../../types/admin";
import RoleModal from "./RoleModal";
import { apiError, TABLE_COLUMNS } from "./roleUtils";

export default function RolesPage() {
  const navigate = useNavigate();

  const [roles, setRoles] = useState<RoleDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<RoleDto | undefined>(undefined);

  useEffect(() => {
    listRoles()
      .then((res) => setRoles(res.data.data ?? []))
      .catch(() => setError("Failed to load roles."))
      .finally(() => setLoading(false));
  }, []);

  const totalBindings = roles.reduce((sum, r) => sum + r.permissions.length, 0);

  function openCreate() {
    setEditing(undefined);
    setModalOpen(true);
  }

  function openEdit(r: RoleDto, e: React.MouseEvent) {
    e.stopPropagation();
    setEditing(r);
    setModalOpen(true);
  }

  function handleSaved(saved: RoleDto) {
    setRoles((prev) => {
      const idx = prev.findIndex((r) => r.id === saved.id);
      if (idx >= 0) {
        const next = [...prev];
        next[idx] = saved;
        return next;
      }
      return [...prev, saved];
    });
  }

  async function handleDelete(r: RoleDto, e: React.MouseEvent) {
    e.stopPropagation();
    if (!globalThis.confirm(`Delete role "${r.name}"? This cannot be undone.`))
      return;
    setDeleteError(null);
    try {
      await deleteRole(r.id);
      setRoles((prev) => prev.filter((x) => x.id !== r.id));
    } catch (err) {
      setDeleteError(apiError(err));
    }
  }

  function renderContent() {
    if (loading) {
      return (
        <div className="flex items-center justify-center py-16 text-sm text-gray-400">
          Loading roles…
        </div>
      );
    }
    if (error) {
      return (
        <div className="flex items-center justify-center py-16 text-sm text-error-600 dark:text-error-400">
          {error}
        </div>
      );
    }
    return (
      <div className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow className="border-b border-gray-100 dark:border-gray-800">
              {TABLE_COLUMNS.map(({ label, className }) => (
                <TableCell
                  key={label || "_actions"}
                  isHeader
                  className={`px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500 dark:text-gray-400 ${className}`}
                >
                  {label}
                </TableCell>
              ))}
            </TableRow>
          </TableHeader>
          <TableBody className="divide-y divide-gray-100 dark:divide-gray-800">
            {roles.length === 0 ? (
              <TableRow>
                <TableCell className="px-6 py-12 text-center text-sm text-gray-400">
                  <div className="flex flex-col items-center gap-2">
                    <LockIcon className="size-8 text-gray-300 dark:text-gray-600" />
                    No roles found
                  </div>
                </TableCell>
              </TableRow>
            ) : (
              roles.map((r) => (
                <TableRow
                  key={r.id}
                  className="hover:bg-gray-50 dark:hover:bg-white/[0.02] cursor-pointer transition-colors"
                  onClick={() => navigate(`/roles/${r.id}`)}
                >
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
                  <TableCell className="px-6 py-3 text-right">
                    <span className="text-sm font-semibold text-gray-800 dark:text-white/90 tabular-nums">
                      {r.permissions.length}
                    </span>
                  </TableCell>
                  <TableCell className="px-6 py-3">
                    <div className="flex items-center justify-end gap-1">
                      <button
                        onClick={(e) => openEdit(r, e)}
                        className="rounded px-2 py-1 text-xs font-medium text-gray-500 hover:text-gray-800 hover:bg-gray-100 dark:text-gray-400 dark:hover:text-white dark:hover:bg-white/5 transition-colors"
                      >
                        Edit
                      </button>
                      <button
                        onClick={(e) => handleDelete(r, e)}
                        className="rounded px-2 py-1 text-xs font-medium text-error-500 hover:text-error-700 hover:bg-error-50 dark:hover:bg-error-500/10 transition-colors"
                      >
                        Delete
                      </button>
                    </div>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>
    );
  }

  return (
    <>
      <PageMeta
        title="Roles | Auth Platform"
        description="Manage roles and their permission assignments"
      />

      {/* Page header */}
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-800 dark:text-white/90">
            Roles
          </h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            {roles.length} roles &middot; {totalBindings} role-permission bindings
          </p>
        </div>
        <button
          onClick={openCreate}
          className="inline-flex items-center gap-2 rounded-lg bg-brand-500 px-4 py-2 text-sm font-medium text-white hover:bg-brand-600 transition-colors"
        >
          <LockIcon className="size-4" />
          New role
        </button>
      </div>

      {/* Delete error banner */}
      {deleteError && (
        <div className="mb-4 rounded-lg border border-error-200 bg-error-50 px-4 py-3 text-sm text-error-700 dark:bg-error-500/10 dark:border-error-500/20 dark:text-error-400">
          {deleteError}
          <button
            onClick={() => setDeleteError(null)}
            className="ml-3 text-xs underline"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Table */}
      <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] overflow-hidden">
        {renderContent()}
      </div>

      {modalOpen && (
        <RoleModal
          initial={editing}
          onClose={() => setModalOpen(false)}
          onSaved={handleSaved}
        />
      )}
    </>
  );
}
