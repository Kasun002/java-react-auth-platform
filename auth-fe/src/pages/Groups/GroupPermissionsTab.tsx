import { memo } from "react";
import Badge from "../../components/ui/badge/Badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHeader,
  TableRow,
} from "../../components/ui/table";
import { BoltIcon } from "../../icons";
import type { PermissionDto } from "../../types/admin";

interface Props {
  allPermissions: PermissionDto[];
}

const HEADERS = ["Permission", "Category", "Description"];

function GroupPermissionsTab({ allPermissions }: Readonly<Props>) {
  return (
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
                  <Badge color="light" size="sm">
                    {p.category}
                  </Badge>
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
  );
}

export default memo(GroupPermissionsTab);
