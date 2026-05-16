/** Badge color variants shared across the UI */
export type BadgeColor = "primary" | "success" | "warning" | "error" | "light";

/** Maps group type → Badge color string (used with <Badge color={...}>) */
export const GROUP_TYPE_BADGE_COLOR: Record<string, BadgeColor> = {
  CUSTOMER: "primary",
  STAFF: "success",
  OVERSIGHT: "warning",
  ADMIN: "error",
};

/** Maps group type → Tailwind bg class (used for chart bars / avatar dots) */
export const GROUP_TYPE_BG_COLOR: Record<string, string> = {
  CUSTOMER: "bg-brand-500",
  STAFF: "bg-success-500",
  OVERSIGHT: "bg-warning-500",
  ADMIN: "bg-error-500",
};
