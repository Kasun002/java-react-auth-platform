import type { PermissionDto } from "../../types/admin";

export {
  getInitials,
  formatDate,
  formatDateTime,
} from "../../utils/formatters";

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
