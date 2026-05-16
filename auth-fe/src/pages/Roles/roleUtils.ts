export type BadgeColor = "primary" | "success" | "warning" | "error" | "light";

export const TYPE_COLOR: Record<string, BadgeColor> = {
  CUSTOMER: "primary",
  STAFF: "success",
  OVERSIGHT: "warning",
  ADMIN: "error",
};

export const TABLE_COLUMNS = [
  { label: "Role", className: "" },
  { label: "Permissions", className: "w-32 text-right" },
  { label: "", className: "w-24" },
];

export function apiError(err: unknown): string {
  const e = err as { response?: { data?: { message?: string } } };
  return e?.response?.data?.message ?? "An unexpected error occurred";
}
