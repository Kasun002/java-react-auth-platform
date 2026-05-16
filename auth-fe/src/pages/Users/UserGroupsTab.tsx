import { memo } from "react";
import Badge from "../../components/ui/badge/Badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHeader,
  TableRow,
} from "../../components/ui/table";
import { GroupIcon } from "../../icons";
import type { UserGroupDto } from "../../types/admin";

const HEADERS = ["Group", "Type", "Description", "Roles", ""];

interface Props {
  groups: UserGroupDto[];
  groupsLoading: boolean;
  isAdUser: boolean;
  onAssignGroup: () => void;
  onRemoveGroup: (groupId: number) => void;
}

function UserGroupsTab({
  groups,
  groupsLoading,
  isAdUser,
  onAssignGroup,
  onRemoveGroup,
}: Readonly<Props>) {
  return (
    <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] overflow-hidden">
      <div className="flex items-center justify-between border-b border-gray-100 dark:border-gray-800 px-6 py-4">
        <h3 className="text-sm font-semibold text-gray-800 dark:text-white/90">
          Group memberships
        </h3>
        {isAdUser ? (
          <span
            title="Managed in Active Directory"
            className="rounded-lg border border-gray-200 px-3 py-1.5 text-xs font-medium text-gray-400 dark:border-gray-700 dark:text-gray-600"
          >
            AD managed
          </span>
        ) : (
          <button
            onClick={onAssignGroup}
            className="rounded-lg bg-brand-500 px-3 py-1.5 text-xs font-medium text-white hover:bg-brand-600 transition-colors"
          >
            + Assign
          </button>
        )}
      </div>
      {groupsLoading ? (
        <div className="flex items-center justify-center py-12 text-sm text-gray-400">
          Loading…
        </div>
      ) : groups.length === 0 ? (
        <div className="flex flex-col items-center py-12 text-gray-400">
          <GroupIcon className="size-10 mb-2 text-gray-300 dark:text-gray-700" />
          <p className="text-sm">No group memberships</p>
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
            {groups.map((g) => (
              <TableRow
                key={g.id}
                className="hover:bg-gray-50 dark:hover:bg-white/[0.02] transition-colors"
              >
                <TableCell className="px-6 py-3 text-sm font-medium text-gray-800 dark:text-white/90">
                  {g.name}
                </TableCell>
                <TableCell className="px-6 py-3">
                  <Badge color="light" size="sm">{g.type}</Badge>
                </TableCell>
                <TableCell className="px-6 py-3 text-sm text-gray-500 dark:text-gray-400">
                  {g.description}
                </TableCell>
                <TableCell className="px-6 py-3 text-sm text-gray-600 dark:text-gray-400 tabular-nums">
                  {g.roles.length}
                </TableCell>
                <TableCell className="px-6 py-3">
                  {!isAdUser && (
                    <button
                      onClick={() => onRemoveGroup(g.id)}
                      className="rounded-lg border border-error-200 px-3 py-1 text-xs font-medium text-error-600 hover:bg-error-50 dark:border-error-500/30 dark:text-error-400 dark:hover:bg-error-500/10 transition-colors"
                    >
                      Remove
                    </button>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </div>
  );
}

export default memo(UserGroupsTab);
