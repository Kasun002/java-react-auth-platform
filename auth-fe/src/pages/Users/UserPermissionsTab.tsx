import { memo } from "react";
import { BoltIcon } from "../../icons";

interface Props {
  permissions: string[];
  loading: boolean;
  error: string | null;
}

function UserPermissionsTab({ permissions, loading, error }: Readonly<Props>) {
  return (
    <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] overflow-hidden">
      <div className="border-b border-gray-100 dark:border-gray-800 px-6 py-4">
        <h3 className="text-sm font-semibold text-gray-800 dark:text-white/90">
          Effective permissions
        </h3>
        <p className="mt-0.5 text-xs text-gray-500 dark:text-gray-400">
          {permissions.length} permission{permissions.length !== 1 ? "s" : ""} — union of all group roles
        </p>
      </div>
      {loading ? (
        <div className="flex items-center justify-center py-12 text-sm text-gray-400">
          Loading…
        </div>
      ) : error ? (
        <div className="px-6 py-4 text-sm text-error-600 dark:text-error-400">{error}</div>
      ) : permissions.length === 0 ? (
        <div className="flex flex-col items-center py-12 text-gray-400">
          <BoltIcon className="size-10 mb-2 text-gray-300 dark:text-gray-700" />
          <p className="text-sm">No permissions found</p>
        </div>
      ) : (
        <div className="p-5">
          <div className="flex flex-wrap gap-2">
            {permissions.map((code) => (
              <span
                key={code}
                className="inline-flex items-center gap-1.5 rounded-lg border border-gray-200 dark:border-gray-700 px-3 py-1.5 text-xs font-mono text-gray-700 dark:text-gray-300"
              >
                <BoltIcon className="size-3 text-brand-500 shrink-0" />
                {code}
              </span>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

export default memo(UserPermissionsTab);
