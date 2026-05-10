// ── Dashboard ─────────────────────────────────────────────────────────────────

export interface DashboardStatsDto {
  kpi: {
    totalUsers: number;
    totalGroups: number;
    totalRoles: number;
    totalPermissions: number;
  };
  users: {
    active: number;
    inactive: number;
    newUsers: number;
    deleted: number;
    localAuth: number;
    azureAdAuth: number;
  };
  groupDistribution: {
    id: number;
    name: string;
    type: string;
    memberCount: number;
  }[];
  permissionsByCategory: {
    category: string;
    count: number;
  }[];
  recentLogins: {
    userId: number;
    userName: string;
    email: string;
    ipAddress: string | null;
    userAgent: string | null;
    issuedAt: string;
    tokenType: string;
  }[];
}

// ── Pagination ────────────────────────────────────────────────────────────────

export interface PageDto<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

// ── RBAC ──────────────────────────────────────────────────────────────────────

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
