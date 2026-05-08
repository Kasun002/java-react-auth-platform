import { useAuth } from "../../context/AuthContext";

export default function UserMetaCard() {
  const { user } = useAuth();
  const initials = user?.name?.charAt(0).toUpperCase() ?? "U";
  const isAD = user?.authProvider === "AZURE_AD";

  return (
    <div className="p-5 border border-gray-200 rounded-2xl dark:border-gray-800 lg:p-6">
      <div className="flex flex-col items-center w-full gap-6 xl:flex-row">
        {/* Avatar */}
        <div className="flex h-20 w-20 flex-shrink-0 items-center justify-center rounded-full bg-brand-500 text-white text-2xl font-bold">
          {initials}
        </div>

        {/* Name + badges */}
        <div>
          <h4 className="mb-2 text-lg font-semibold text-center text-gray-800 dark:text-white/90 xl:text-left">
            {user?.name ?? "—"}
          </h4>
          <div className="flex flex-wrap items-center justify-center gap-2 xl:justify-start">
            <span className="inline-flex items-center rounded-full bg-brand-50 px-3 py-1 text-xs font-medium text-brand-700 dark:bg-brand-500/10 dark:text-brand-400">
              {user?.role ?? "USER"}
            </span>
            <span className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-medium ${
              user?.status === "ACTIVE"
                ? "bg-success-50 text-success-700 dark:bg-success-500/10 dark:text-success-400"
                : "bg-error-50 text-error-700 dark:bg-error-500/10 dark:text-error-400"
            }`}>
              {user?.status ?? "UNKNOWN"}
            </span>
            <span className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-medium ${
              isAD
                ? "bg-blue-50 text-blue-700 dark:bg-blue-500/10 dark:text-blue-400"
                : "bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-400"
            }`}>
              {isAD ? "Azure AD" : "Local"}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}
