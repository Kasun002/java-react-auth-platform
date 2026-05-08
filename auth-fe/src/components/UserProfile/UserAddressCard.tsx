import { useAuth } from "../../context/AuthContext";

export default function UserAddressCard() {
  const { user } = useAuth();

  const hasRoles = user?.roles && user.roles.length > 0;
  const hasPermissions = user?.effectivePermissions && user.effectivePermissions.length > 0;

  return (
    <div className="p-5 border border-gray-200 rounded-2xl dark:border-gray-800 lg:p-6">
      <h4 className="mb-6 text-lg font-semibold text-gray-800 dark:text-white/90">
        Roles &amp; Permissions
      </h4>

      <div className="space-y-5">
        <div>
          <p className="mb-2 text-xs leading-normal text-gray-500 dark:text-gray-400">
            Assigned Roles
          </p>
          {hasRoles ? (
            <div className="flex flex-wrap gap-2">
              {user!.roles.map((r) => (
                <span
                  key={r}
                  className="inline-flex items-center rounded-full bg-brand-50 px-3 py-1 text-xs font-medium text-brand-700 dark:bg-brand-500/10 dark:text-brand-400"
                >
                  {r}
                </span>
              ))}
            </div>
          ) : (
            <p className="text-sm font-medium text-gray-800 dark:text-white/90">—</p>
          )}
        </div>

        <div>
          <p className="mb-2 text-xs leading-normal text-gray-500 dark:text-gray-400">
            Effective Permissions
          </p>
          {hasPermissions ? (
            <div className="flex flex-wrap gap-2">
              {user!.effectivePermissions.map((p) => (
                <span
                  key={p}
                  className="inline-flex items-center rounded-full bg-gray-100 px-3 py-1 text-xs font-medium text-gray-600 dark:bg-gray-800 dark:text-gray-400"
                >
                  {p}
                </span>
              ))}
            </div>
          ) : (
            <p className="text-sm font-medium text-gray-800 dark:text-white/90">—</p>
          )}
        </div>
      </div>
    </div>
  );
}
