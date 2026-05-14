import { memo } from "react";
import { AlertIcon, CheckCircleIcon, ErrorIcon } from "../../icons";

interface Props {
  successCount: number;
  warningCount: number;
  failureCount: number;
}

const chip =
  "flex items-center gap-2 rounded-lg border border-gray-200 dark:border-gray-700 px-3 py-2";
const label = "text-sm text-gray-600 dark:text-gray-400";
const count =
  "text-sm font-semibold text-gray-800 dark:text-white/90 tabular-nums";

function AuditSummaryChips({
  successCount,
  warningCount,
  failureCount,
}: Readonly<Props>) {
  return (
    <div className="mb-4 flex flex-wrap gap-2">
      <div className={chip}>
        <CheckCircleIcon className="size-4 text-success-500" />
        <span className={label}>Success</span>
        <span className={count}>{successCount}</span>
      </div>
      <div className={chip}>
        <AlertIcon className="size-4 text-warning-500" />
        <span className={label}>Warning</span>
        <span className={count}>{warningCount}</span>
      </div>
      <div className={chip}>
        <ErrorIcon className="size-4 text-error-500" />
        <span className={label}>Failure</span>
        <span className={count}>{failureCount}</span>
      </div>
    </div>
  );
}

export default memo(AuditSummaryChips);
