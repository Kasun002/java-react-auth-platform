import api from "../lib/axios";
import type { ApiResponse, UserDto } from "../types/auth";
import type { BankingRoleDto, DashboardStatsDto, PageDto, PermissionDto, UserGroupDto } from "../types/admin";

// ── Dashboard ─────────────────────────────────────────────────────────────────

export const getDashboardStats = () =>
  api.get<ApiResponse<DashboardStatsDto>>("/admin/dashboard/stats");

// ── Permissions ───────────────────────────────────────────────────────────────

export const listPermissions = () =>
  api.get<ApiResponse<PermissionDto[]>>("/admin/permissions");

// ── Roles ─────────────────────────────────────────────────────────────────────

export const listRoles = () =>
  api.get<ApiResponse<BankingRoleDto[]>>("/admin/roles");

export const getRole = (id: number) =>
  api.get<ApiResponse<BankingRoleDto>>(`/admin/roles/${id}`);

export const assignPermissionToRole = (roleId: number, permissionId: number) =>
  api.post<ApiResponse<BankingRoleDto>>(`/admin/roles/${roleId}/permissions`, { permissionId });

export const removePermissionFromRole = (roleId: number, permissionId: number) =>
  api.delete<void>(`/admin/roles/${roleId}/permissions/${permissionId}`);

// ── Groups ────────────────────────────────────────────────────────────────────

export const listGroups = () =>
  api.get<ApiResponse<UserGroupDto[]>>("/admin/groups");

export const getGroup = (id: number) =>
  api.get<ApiResponse<UserGroupDto>>(`/admin/groups/${id}`);

export const assignRoleToGroup = (groupId: number, roleId: number) =>
  api.post<ApiResponse<UserGroupDto>>(`/admin/groups/${groupId}/roles`, { roleId });

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