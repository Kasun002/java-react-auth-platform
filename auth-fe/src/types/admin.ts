export interface PermissionDto {
  id: number;
  code: string;
  description: string;
  category: string;
}

export interface BankingRoleDto {
  id: number;
  name: string;
  description: string;
  permissions: PermissionDto[];
}

export interface UserGroupDto {
  id: number;
  name: string;
  description: string;
  type: "CUSTOMER" | "STAFF" | "OVERSIGHT" | "ADMIN";
  roles: BankingRoleDto[];
}
