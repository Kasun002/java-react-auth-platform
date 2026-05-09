import { Link } from "react-router";
import GridShape from "../../components/common/GridShape";
import PageMeta from "../../components/common/PageMeta";

export default function Unauthorized() {
  return (
    <>
      <PageMeta
        title="403 Unauthorized | Admin Panel"
        description="You do not have permission to access this page."
      />
      <div className="relative flex flex-col items-center justify-center min-h-screen p-6 overflow-hidden z-1">
        <GridShape />
        <div className="mx-auto w-full max-w-[472px] text-center">
          <h1 className="mb-4 font-bold text-gray-800 text-title-md dark:text-white/90 xl:text-title-2xl">
            403
          </h1>
          <h2 className="mb-4 text-xl font-semibold text-gray-700 dark:text-gray-300">
            Access Denied
          </h2>
          <p className="mb-8 text-base text-gray-600 dark:text-gray-400">
            You need <strong>SYSTEM_ADMIN</strong> or <strong>SUPER_ADMIN</strong> group
            membership to access the admin panel. Contact your system administrator to
            request access.
          </p>
          <Link
            to="/profile"
            className="inline-flex items-center justify-center rounded-lg border border-gray-300 bg-white px-5 py-3.5 text-sm font-medium text-gray-700 shadow-theme-xs hover:bg-gray-50 hover:text-gray-800 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-400 dark:hover:bg-white/[0.03] dark:hover:text-gray-200"
          >
            Go to Profile
          </Link>
        </div>
        <p className="absolute text-sm text-center text-gray-500 -translate-x-1/2 bottom-6 left-1/2 dark:text-gray-400">
          &copy; {new Date().getFullYear()} - Admin Panel
        </p>
      </div>
    </>
  );
}
