import { useAuth } from "../../context/AuthContext";

function formatDate(iso: string | null | undefined) {
  if (!iso) return "—";
  return new Date(iso).toLocaleString();
}

export default function UserInfoCard() {
  const { user } = useAuth();

  const fields = [
    { label: "Full Name",      value: user?.name },
    { label: "Email Address",  value: user?.email },
    { label: "Role",           value: user?.role },
    { label: "Status",         value: user?.status },
    { label: "Auth Provider",  value: user?.authProvider === "AZURE_AD" ? "Azure AD" : "Local" },
    { label: "Last Login",     value: formatDate(user?.lastLoginAt) },
    { label: "Member Since",   value: formatDate(user?.createdAt) },
    ...(user?.groups?.length ? [{ label: "Groups", value: user.groups.join(", ") }] : []),
  ];

  return (
    <div className="p-5 border border-gray-200 rounded-2xl dark:border-gray-800 lg:p-6">
      <h4 className="mb-6 text-lg font-semibold text-gray-800 dark:text-white/90">
        Account Information
      </h4>
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2 lg:gap-7 2xl:gap-x-32">
        {fields.map(({ label, value }) => (
          <div key={label}>
            <p className="mb-1 text-xs leading-normal text-gray-500 dark:text-gray-400">
              {label}
            </p>
            <p className="text-sm font-medium text-gray-800 dark:text-white/90">
              {value ?? "—"}
            </p>
          </div>
        ))}
      </div>
    </div>
  );
}
