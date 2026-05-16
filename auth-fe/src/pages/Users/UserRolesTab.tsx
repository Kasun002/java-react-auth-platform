import { memo } from "react";
import Badge from "../../components/ui/badge/Badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHeader,
  TableRow,
} from "../../components/ui/table";
import { LockIcon } from "../../icons";
import type { RoleWithSource } from "./userUtils";

const HEADERS = ["Role", "Description", "Source", "Permissions"];

interface Props {
  uniqueRoles: RoleWithSource[];
}

function UserRolesTab({ uniqueRoles }: Readonly<Props>) {
  return (
    <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] overflow-hidden">
      <div className="border-b border-gray-100 dark:border-gray-800 px-6 py-4">
        <h3 className="text-sm font-semibold text-gray-800 dark:text-white/90">
          Effective roles
        </h3>
        <p className="mt-0.5 text-xs text-gray-500 dark:text-gray-400">
          Inherited via group memberships — permissions update on next login
        </p>
      </div>
      {uniqueRoles.length === 0 ? (
        <div className="flex flex-col items-center py-12 text-gray-400">
          <LockIcon className="size-10 mb-2 text-gray-300 dark:text-gray-700" />
          <p className="text-sm">No roles assigned</p>
        </div>
      ) : (
        <Table>
          <TableHeader>
            <TableRow className="border-b border-gray-100 dark:border-gray-800">
              {HEADERS.map((h) => (
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
            {uniqueRoles.map((r) => (
              <TableRow
                key={r.id}
                className="hover:bg-gray-50 dark:hover:bg-white/[0.02] transition-colors"
              >
                <TableCell className="px-6 py-3">
                  <div className="flex items-center gap-2">
                    <LockIcon className="size-3.5 text-warning-500 shrink-0" />
                    <span className="text-sm font-medium text-gray-800 dark:text-white/90 font-mono">
                      {r.name}
                    </span>
                  </div>
                </TableCell>
                <TableCell className="px-6 py-3 text-sm text-gray-500 dark:text-gray-400">
                  {r.description}
                </TableCell>
                <TableCell className="px-6 py-3">
                  <Badge color="primary" size="sm">via {r.sourceGroup}</Badge>
                </TableCell>
                <TableCell className="px-6 py-3 text-sm tabular-nums text-gray-600 dark:text-gray-400">
                  {r.permissions.length}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </div>
  );
}

export default memo(UserRolesTab);
