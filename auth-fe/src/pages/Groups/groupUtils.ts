export type BadgeColor = "primary" | "success" | "warning" | "error" | "light";

export const PRESET_COLORS: Record<string, BadgeColor> = {
  CUSTOMER: "primary",
  STAFF: "success",
  OVERSIGHT: "warning",
  ADMIN: "error",
};

export function typeColor(type: string): BadgeColor {
  return PRESET_COLORS[type.toUpperCase()] ?? "light";
}

export function apiError(err: unknown): string {
  const e = err as { response?: { data?: { message?: string } } };
  return e?.response?.data?.message ?? "An unexpected error occurred";
}

// Shared CSS constants used across Group modals
export const inputCls =
  "w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 placeholder-gray-400 focus:border-brand-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-white/90";

export const cancelBtnCls =
  "rounded-lg border border-gray-200 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-white/5";

export const saveBtnCls =
  "rounded-lg bg-brand-500 px-4 py-2 text-sm font-medium text-white hover:bg-brand-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors";

export const errorBannerCls =
  "rounded-lg border border-error-200 bg-error-50 px-4 py-2 text-sm text-error-700 dark:bg-error-500/10 dark:border-error-500/20 dark:text-error-400";
