// ── Audit Log ─────────────────────────────────────────────────────────────────

export interface AuditLogDto {
  id: number;
  actorId: number;
  actorName: string;
  action: string;
  resource: string;
  resourceId: string | null;
  details: string | null;
  ipAddress: string | null;
  status: "SUCCESS" | "WARNING" | "FAILURE";
  createdAt: string;
}

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

export interface RoleDto {
  id: number;
  name: string;
  description: string;
  permissions: PermissionDto[];
}

export interface UserGroupDto {
  id: number;
  name: string;
  description: string;
  type: string;   // free-form — defined by the organization
  roles: RoleDto[];
}

// ── CRUD Requests ─────────────────────────────────────────────────────────────

export interface CreatePermissionRequest {
  code: string;
  category: string;
  description?: string;
}

export interface UpdatePermissionRequest {
  code: string;
  category: string;
  description?: string;
}

export interface CreateRoleRequest {
  name: string;
  description?: string;
}

export interface UpdateRoleRequest {
  name: string;
  description?: string;
}

export interface CreateGroupRequest {
  name: string;
  type: string;
  description?: string;
}

export interface UpdateGroupRequest {
  name: string;
  type: string;
  description?: string;
}

// ── User management requests ───────────────────────────────────────────────────

export interface AdminCreateUserRequest {
  name: string;
  email: string;
  temporaryPassword: string;
  phone?: string;
  groupIds: number[];
  roleIds: number[];
}

export interface AdminUpdateUserRequest {
  name: string;
  email: string;
  phone?: string;
}

export interface UpdateUserStatusRequest {
  status: "ACTIVE" | "INACTIVE" | "SUSPENDED";
}
