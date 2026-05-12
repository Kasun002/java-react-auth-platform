import api from "../lib/axios";
import type { ApiResponse, UserDto } from "../types/auth";
import type {
  AuditLogDto,
  RoleDto,
  DashboardStatsDto,
  PageDto,
  PermissionDto,
  UserGroupDto,
  CreatePermissionRequest,
  UpdatePermissionRequest,
  CreateRoleRequest,
  UpdateRoleRequest,
  CreateGroupRequest,
  UpdateGroupRequest,
} from "../types/admin";

// ── Audit Log ─────────────────────────────────────────────────────────────────

export const getAuditLogs = (params: {
  page: number;
  size: number;
  status?: string;
  q?: string;
}) => api.get<ApiResponse<PageDto<AuditLogDto>>>("/admin/audit-logs", { params });

// ── Dashboard ─────────────────────────────────────────────────────────────────

export const getDashboardStats = () =>
  api.get<ApiResponse<DashboardStatsDto>>("/admin/dashboard/stats");

// ── Permissions ───────────────────────────────────────────────────────────────

export const listPermissions = () =>
  api.get<ApiResponse<PermissionDto[]>>("/admin/permissions");

export const createPermission = (data: CreatePermissionRequest) =>
  api.post<ApiResponse<PermissionDto>>("/admin/permissions", data);

export const updatePermission = (id: number, data: UpdatePermissionRequest) =>
  api.put<ApiResponse<PermissionDto>>(`/admin/permissions/${id}`, data);

export const deletePermission = (id: number) =>
  api.delete<void>(`/admin/permissions/${id}`);

// ── Roles ─────────────────────────────────────────────────────────────────────

export const listRoles = () => api.get<ApiResponse<RoleDto[]>>("/admin/roles");

export const getRole = (id: number) =>
  api.get<ApiResponse<RoleDto>>(`/admin/roles/${id}`);

export const createRole = (data: CreateRoleRequest) =>
  api.post<ApiResponse<RoleDto>>("/admin/roles", data);

export const updateRole = (id: number, data: UpdateRoleRequest) =>
  api.put<ApiResponse<RoleDto>>(`/admin/roles/${id}`, data);

export const deleteRole = (id: number) =>
  api.delete<void>(`/admin/roles/${id}`);

export const assignPermissionToRole = (roleId: number, permissionId: number) =>
  api.post<ApiResponse<RoleDto>>(`/admin/roles/${roleId}/permissions`, {
    permissionId,
  });

export const removePermissionFromRole = (
  roleId: number,
  permissionId: number
) => api.delete<void>(`/admin/roles/${roleId}/permissions/${permissionId}`);

// ── Groups ────────────────────────────────────────────────────────────────────

export const listGroups = () =>
  api.get<ApiResponse<UserGroupDto[]>>("/admin/groups");

export const getGroup = (id: number) =>
  api.get<ApiResponse<UserGroupDto>>(`/admin/groups/${id}`);

export const createGroup = (data: CreateGroupRequest) =>
  api.post<ApiResponse<UserGroupDto>>("/admin/groups", data);

export const updateGroup = (id: number, data: UpdateGroupRequest) =>
  api.put<ApiResponse<UserGroupDto>>(`/admin/groups/${id}`, data);

export const deleteGroup = (id: number) =>
  api.delete<void>(`/admin/groups/${id}`);

export const assignRoleToGroup = (groupId: number, roleId: number) =>
  api.post<ApiResponse<UserGroupDto>>(`/admin/groups/${groupId}/roles`, {
    roleId,
  });

export const removeRoleFromGroup = (groupId: number, roleId: number) =>
  api.delete<void>(`/admin/groups/${groupId}/roles/${roleId}`);

// ── User ↔ Group membership ───────────────────────────────────────────────────

export const getUserGroups = (userId: number) =>
  api.get<ApiResponse<UserGroupDto[]>>(`/admin/users/${userId}/groups`);

export const addUserToGroup = (userId: number, groupId: number) =>
  api.post<ApiResponse<void>>(`/admin/users/${userId}/groups`, { groupId });

export const removeUserFromGroup = (userId: number, groupId: number) =>
  api.delete<void>(`/admin/users/${userId}/groups/${groupId}`);

// ── Effective permissions ─────────────────────────────────────────────────────

export const getUserEffectivePermissions = (userId: number) =>
  api.get<ApiResponse<string[]>>(`/admin/users/${userId}/permissions`);

// ── Users ─────────────────────────────────────────────────────────────────────

export const getUsers = (params: { page: number; size: number }) =>
  api.get<ApiResponse<PageDto<UserDto>>>("/admin/users", { params });

export const getUserById = (userId: number) =>
  api.get<ApiResponse<UserDto>>(`/admin/users/${userId}`);
