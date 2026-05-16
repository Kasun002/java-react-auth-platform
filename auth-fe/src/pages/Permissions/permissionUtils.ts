export function apiError(err: unknown): string {
  const e = err as { response?: { data?: { message?: string } } };
  return e?.response?.data?.message ?? "An unexpected error occurred";
}

export const TABLE_COLUMNS = [
  { label: "Code", className: "w-64" },
  { label: "Category", className: "w-40 hidden md:table-cell" },
  { label: "Description", className: "" },
  { label: "Roles", className: "w-20 text-right hidden lg:table-cell" },
  { label: "", className: "w-24" },
];
