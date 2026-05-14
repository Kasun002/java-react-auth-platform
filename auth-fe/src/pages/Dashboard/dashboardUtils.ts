export function formatTime(iso: string) {
  return new Date(iso).toLocaleString(undefined, {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function getInitials(name: string) {
  return name
    .split(" ")
    .map((n) => n[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();
}

export const GROUP_TYPE_COLOR: Record<string, string> = {
  CUSTOMER: "bg-brand-500",
  STAFF: "bg-success-500",
  OVERSIGHT: "bg-warning-500",
  ADMIN: "bg-error-500",
};
