import type { PermissionDto } from "../../types/admin";

export const PAGE_SIZE = 10;

export type UserStatusColor = "success" | "error" | "warning" | "light";

export const STATUS_COLOR: Record<string, UserStatusColor> = {
  ACTIVE: "success",
  INACTIVE: "light",
  SUSPENDED: "error",
  NEW: "warning",
};

export const AUTH_PROVIDER_LABEL: Record<string, string> = {
  LOCAL: "Local",
  AZURE_AD: "Azure AD",
};

export const AUTH_PROVIDER_COLOR: Record<string, string> = {
  AZURE_AD: "primary",
  LOCAL: "light",
};

export interface RoleWithSource {
  id: number;
  name: string;
  description?: string;
  permissions: PermissionDto[];
  sourceGroup: string;
}

export function getInitials(name: string): string {
  return name
    .split(" ")
    .map((n) => n[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();
}

export function formatDate(iso: string | null): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString(undefined, {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
}

export function formatDateTime(iso: string | null): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleString(undefined, {
    month: "short",
    day: "numeric",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}
