export { getInitials } from "../../utils/formatters";
export { formatTimestamp as formatDate } from "../../utils/formatters";

export const PAGE_SIZE = Number(import.meta.env.VITE_AUDIT_PAGE_SIZE ?? 10);

export const TABLE_COLUMNS = [
  { label: "Actor",   className: "" },
  { label: "Action",  className: "hidden md:table-cell" },
  { label: "Details", className: "hidden lg:table-cell" },
  { label: "IP",      className: "w-32 hidden xl:table-cell" },
  { label: "Time",    className: "w-44 hidden md:table-cell" },
  { label: "Status",  className: "w-28" },
] as const;
