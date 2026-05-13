import { memo } from "react";
import Badge from "../../components/ui/badge/Badge";
import { TableCell, TableRow } from "../../components/ui/table";
import { AlertIcon, CheckCircleIcon, ErrorIcon } from "../../icons";
import type { AuditLogDto } from "../../types/admin";
import { formatDate, getInitials } from "./auditUtils";

interface Props {
  entry: AuditLogDto;
}

const STATUS_ICON = {
  SUCCESS: <CheckCircleIcon className="size-3.5 text-success-500 shrink-0" />,
  WARNING: <AlertIcon className="size-3.5 text-warning-500 shrink-0" />,
  FAILURE: <ErrorIcon className="size-3.5 text-error-500  shrink-0" />,
} as const;

const STATUS_BADGE = {
  SUCCESS: (
    <Badge color="success" size="sm">
      Success
    </Badge>
  ),
  WARNING: (
    <Badge color="warning" size="sm">
      Warning
    </Badge>
  ),
  FAILURE: (
    <Badge color="error" size="sm">
      Failure
    </Badge>
  ),
} as const;

function AuditTableRow({ entry }: Readonly<Props>) {
  return (
    <TableRow className="hover:bg-gray-50 dark:hover:bg-white/[0.02] transition-colors">
      {/* Actor */}
      <TableCell className="px-6 py-3">
        <div className="flex items-center gap-2.5">
          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-brand-50 dark:bg-brand-500/15 text-brand-600 dark:text-brand-400 text-xs font-semibold">
            {getInitials(entry.actorName)}
          </div>
          <div className="min-w-0">
            <p className="text-sm font-medium text-gray-800 dark:text-white/90 truncate">
              {entry.actorName.split(" ")[0]}
            </p>
            <p className="text-xs text-gray-400 dark:text-gray-500 font-mono">
              {entry.resource}
            </p>
          </div>
        </div>
      </TableCell>

      {/* Action */}
      <TableCell className="px-6 py-3 hidden md:table-cell">
        <div className="flex items-center gap-1.5">
          {STATUS_ICON[entry.status]}
          <span className="text-xs font-mono text-gray-700 dark:text-gray-300 whitespace-nowrap">
            {entry.action.replace(/_/g, " ")}
          </span>
        </div>
      </TableCell>

      {/* Details */}
      <TableCell className="px-6 py-3 hidden lg:table-cell">
        <p className="text-sm text-gray-500 dark:text-gray-400 break-words max-w-xs">
          {entry.details}
        </p>
      </TableCell>

      {/* IP */}
      <TableCell className="px-6 py-3 text-xs font-mono text-gray-500 dark:text-gray-400 hidden xl:table-cell">
        {entry.ipAddress ?? "—"}
      </TableCell>

      {/* Time */}
      <TableCell className="px-6 py-3 text-xs font-mono text-gray-500 dark:text-gray-400 whitespace-nowrap hidden md:table-cell">
        {formatDate(entry.createdAt)}
      </TableCell>

      {/* Status */}
      <TableCell className="px-6 py-3">{STATUS_BADGE[entry.status]}</TableCell>
    </TableRow>
  );
}

export default memo(AuditTableRow);
