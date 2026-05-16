import { memo } from "react";
import {
  Table,
  TableBody,
  TableCell,
  TableHeader,
  TableRow,
} from "../../components/ui/table";
import { LockIcon } from "../../icons";
import type { RoleDto } from "../../types/admin";

interface Props {
  roles: RoleDto[];
  onAssignRole: () => void;
  onRemoveRole: (roleId: number) => void;
}

const HEADERS = ["Role", "Description", "Permissions", ""];

function GroupRolesTab({ roles, onAssignRole, onRemoveRole }: Readonly<Props>) {
  return (
    <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] overflow-hidden">
      <div className="flex items-center justify-between border-b border-gray-100 dark:border-gray-800 px-6 py-4">
        <h3 className="text-sm font-semibold text-gray-800 dark:text-white/90">
          Assigned roles
        </h3>
        <button
          onClick={onAssignRole}
          className="rounded-lg bg-brand-500 px-3 py-1.5 text-xs font-medium text-white hover:bg-brand-600 transition-colors"
        >
          + Assign role
        </button>
      </div>
      {roles.length === 0 ? (
        <div className="flex flex-col items-center py-12 text-gray-400">
          <LockIcon className="size-10 mb-2 text-gray-300 dark:text-gray-700" />
          <p className="text-sm">No roles assigned to this group</p>
        </div>
      ) : (
        <Table>
          <TableHeader>
            <TableRow className="border-b border-gray-100 dark:border-gray-800">
              {HEADERS.map((h) => (
                <TableCell
                  key={h || "_actions"}
                  isHeader
                  className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500 dark:text-gray-400"
                >
                  {h}
                </TableCell>
              ))}
            </TableRow>
          </TableHeader>
          <TableBody className="divide-y divide-gray-100 dark:divide-gray-800">
            {roles.map((r) => (
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
                    onClick={() => onRemoveRole(r.id)}
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
  );
}

export default memo(GroupRolesTab);
