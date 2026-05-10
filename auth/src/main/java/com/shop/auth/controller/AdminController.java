package com.shop.auth.controller;

import java.util.List;
import java.util.Set;

import com.shop.auth.dto.AssignGroupRequestDto;
import com.shop.auth.dto.AssignPermissionToRoleRequestDto;
import com.shop.auth.dto.AssignRoleToGroupRequestDto;
import com.shop.auth.dto.BankingRoleDto;
import com.shop.auth.dto.PageDto;
import com.shop.auth.dto.PermissionDto;
import com.shop.auth.dto.ResponseDto;
import com.shop.auth.dto.UserDto;
import com.shop.auth.dto.UserGroupDto;
import com.shop.auth.service.AuthService;
import com.shop.auth.service.BankingRoleService;
import com.shop.auth.service.PermissionService;
import com.shop.auth.service.UserGroupService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin API — RBAC management endpoints for permissions, roles, groups, and
 * user membership.
 *
 * <p>
 * All endpoints require a valid ACCESS JWT. Fine-grained authority checks are
 * enforced
 * via {@code @PreAuthorize}. Any request without sufficient authority returns
 * 403.
 * </p>
 *
 * <p>
 * Banking standards: PCI-DSS v4 Req 7.2 (least privilege), ISO 27001 A.9.4.2
 * (separation of duties). All admin actions are logged via MDC correlation ID.
 * </p>
 */
@Slf4j
@Tag(name = "Admin — RBAC", description = "Role, group, permission management. Requires SYSTEM_ADMIN or SUPER_ADMIN access.")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final PermissionService permissionService;
    private final BankingRoleService bankingRoleService;
    private final UserGroupService userGroupService;
    private final AuthService authService;

    // ══════════════════════════════════════════════════════════════════════════
    // Permissions
    // ══════════════════════════════════════════════════════════════════════════

    @Operation(summary = "List all permissions", description = "Returns all atomic permission codes sorted by category and code.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Permission list returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient authority", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('PERMISSION_MANAGE')")
    public ResponseEntity<ResponseDto<List<PermissionDto>>> listPermissions() {
        log.info("Admin: listing all permissions");
        List<PermissionDto> data = permissionService.listAll();

        ResponseDto<List<PermissionDto>> response = new ResponseDto<>();
        response.setStatus(ResponseDto.Status.SUCCESS);
        response.setData(data);
        response.setMessage(data.size() + " permission(s) found");
        return ResponseEntity.ok(response);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Roles
    // ══════════════════════════════════════════════════════════════════════════

    @Operation(summary = "List all roles", description = "Returns all banking roles with their assigned permissions.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role list returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient authority", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<ResponseDto<List<BankingRoleDto>>> listRoles() {
        log.info("Admin: listing all banking roles");
        List<BankingRoleDto> data = bankingRoleService.listAll();

        ResponseDto<List<BankingRoleDto>> response = new ResponseDto<>();
        response.setStatus(ResponseDto.Status.SUCCESS);
        response.setData(data);
        response.setMessage(data.size() + " role(s) found");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get role by ID", description = "Returns a single banking role with its permissions.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient authority", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Role not found", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<ResponseDto<BankingRoleDto>> getRole(
            @Parameter(description = "Role ID") @PathVariable Long id) {
        log.info("Admin: fetching role id=[{}]", id);
        BankingRoleDto data = bankingRoleService.getById(id);

        ResponseDto<BankingRoleDto> response = new ResponseDto<>();
        response.setStatus(ResponseDto.Status.SUCCESS);
        response.setData(data);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Assign permission to role", description = "Adds a permission to a role. Idempotent — no error if already assigned.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Permission assigned — updated role returned"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient authority", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Role or permission not found", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('PERMISSION_MANAGE')")
    public ResponseEntity<ResponseDto<BankingRoleDto>> assignPermissionToRole(
            @Parameter(description = "Role ID") @PathVariable Long id,
            @Valid @RequestBody AssignPermissionToRoleRequestDto request) {
        log.info("Admin: assigning permission id=[{}] to role id=[{}]", request.getPermissionId(), id);
        BankingRoleDto data = bankingRoleService.assignPermission(id, request.getPermissionId());

        ResponseDto<BankingRoleDto> response = new ResponseDto<>();
        response.setStatus(ResponseDto.Status.SUCCESS);
        response.setMessage("Permission assigned successfully");
        response.setData(data);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Remove permission from role", description = "Removes a permission from a role. No-op if not currently assigned.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Permission removed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient authority", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Role or permission not found", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @DeleteMapping("/roles/{id}/permissions/{permissionId}")
    @PreAuthorize("hasAuthority('PERMISSION_MANAGE')")
    public ResponseEntity<Void> removePermissionFromRole(
            @Parameter(description = "Role ID") @PathVariable Long id,
            @Parameter(description = "Permission ID") @PathVariable Long permissionId) {
        log.info("Admin: removing permission id=[{}] from role id=[{}]", permissionId, id);
        bankingRoleService.removePermission(id, permissionId);
        return ResponseEntity.noContent().build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Groups
    // ══════════════════════════════════════════════════════════════════════════

    @Operation(summary = "List all groups", description = "Returns all user groups with their assigned roles.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Group list returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient authority", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/groups")
    @PreAuthorize("hasAuthority('GROUP_MANAGE')")
    public ResponseEntity<ResponseDto<List<UserGroupDto>>> listGroups() {
        log.info("Admin: listing all user groups");
        List<UserGroupDto> data = userGroupService.listAll();

        ResponseDto<List<UserGroupDto>> response = new ResponseDto<>();
        response.setStatus(ResponseDto.Status.SUCCESS);
        response.setData(data);
        response.setMessage(data.size() + " group(s) found");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get group by ID", description = "Returns a single group with its roles and permissions.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Group returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient authority", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Group not found", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/groups/{id}")
    @PreAuthorize("hasAuthority('GROUP_MANAGE')")
    public ResponseEntity<ResponseDto<UserGroupDto>> getGroup(
            @Parameter(description = "Group ID") @PathVariable Long id) {
        log.info("Admin: fetching group id=[{}]", id);
        UserGroupDto data = userGroupService.getById(id);

        ResponseDto<UserGroupDto> response = new ResponseDto<>();
        response.setStatus(ResponseDto.Status.SUCCESS);
        response.setData(data);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Assign role to group", description = "Adds a banking role to a group. Idempotent — no error if already assigned.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role assigned — updated group returned"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient authority", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Group or role not found", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("/groups/{id}/roles")
    @PreAuthorize("hasAuthority('GROUP_MANAGE')")
    public ResponseEntity<ResponseDto<UserGroupDto>> assignRoleToGroup(
            @Parameter(description = "Group ID") @PathVariable Long id,
            @Valid @RequestBody AssignRoleToGroupRequestDto request) {
        log.info("Admin: assigning role id=[{}] to group id=[{}]", request.getRoleId(), id);
        UserGroupDto data = userGroupService.assignRoleToGroup(id, request.getRoleId());

        ResponseDto<UserGroupDto> response = new ResponseDto<>();
        response.setStatus(ResponseDto.Status.SUCCESS);
        response.setMessage("Role assigned to group successfully");
        response.setData(data);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Remove role from group", description = "Removes a role from a group. No-op if the role is not assigned.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Role removed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient authority", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Group or role not found", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @DeleteMapping("/groups/{id}/roles/{roleId}")
    @PreAuthorize("hasAuthority('GROUP_MANAGE')")
    public ResponseEntity<Void> removeRoleFromGroup(
            @Parameter(description = "Group ID") @PathVariable Long id,
            @Parameter(description = "Role ID") @PathVariable Long roleId) {
        log.info("Admin: removing role id=[{}] from group id=[{}]", roleId, id);
        userGroupService.removeRoleFromGroup(id, roleId);
        return ResponseEntity.noContent().build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // User ↔ Group membership
    // ══════════════════════════════════════════════════════════════════════════

    @Operation(summary = "Get user group memberships", description = "Returns all groups the user currently belongs to.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Group list returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient authority", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/users/{userId}/groups")
    @PreAuthorize("hasAuthority('USER_GROUPS_MANAGE')")
    public ResponseEntity<ResponseDto<List<UserGroupDto>>> getUserGroups(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        log.info("Admin: fetching groups for user id=[{}]", userId);
        List<UserGroupDto> data = userGroupService.getUserGroups(userId);

        ResponseDto<List<UserGroupDto>> response = new ResponseDto<>();
        response.setStatus(ResponseDto.Status.SUCCESS);
        response.setData(data);
        response.setMessage(data.size() + " group(s) found for user");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Add user to group", description = "Assigns a user to a group. Idempotent — no error if already a member.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User added to group"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient authority", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "User or group not found", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("/users/{userId}/groups")
    @PreAuthorize("hasAuthority('USER_GROUPS_MANAGE')")
    public ResponseEntity<ResponseDto<Void>> addUserToGroup(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Valid @RequestBody AssignGroupRequestDto request) {
        log.info("Admin: adding user id=[{}] to group id=[{}]", userId, request.getGroupId());
        userGroupService.addUserToGroup(userId, request.getGroupId());

        ResponseDto<Void> response = new ResponseDto<>();
        response.setStatus(ResponseDto.Status.SUCCESS);
        response.setMessage("User added to group successfully");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Remove user from group", description = "Removes a user from a group. No-op if the user is not a member.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User removed from group"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient authority", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "User or group not found", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @DeleteMapping("/users/{userId}/groups/{groupId}")
    @PreAuthorize("hasAuthority('USER_GROUPS_MANAGE')")
    public ResponseEntity<Void> removeUserFromGroup(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Parameter(description = "Group ID") @PathVariable Long groupId) {
        log.info("Admin: removing user id=[{}] from group id=[{}]", userId, groupId);
        userGroupService.removeUserFromGroup(userId, groupId);
        return ResponseEntity.noContent().build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Effective permissions
    // ══════════════════════════════════════════════════════════════════════════

    @Operation(summary = "Get user effective permissions", description = "Returns the complete set of permission codes a user holds — "
            + "union of permissions from all group-assigned roles and direct role assignments. "
            + "Use for periodic access reviews (PCI-DSS v4 Req 7.2).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Permission set returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient authority", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/users/{userId}/permissions")
    @PreAuthorize("hasAuthority('USER_GROUPS_MANAGE')")
    public ResponseEntity<ResponseDto<Set<String>>> getUserEffectivePermissions(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        log.info("Admin: fetching effective permissions for user id=[{}]", userId);
        Set<String> data = userGroupService.getEffectivePermissions(userId);

        ResponseDto<Set<String>> response = new ResponseDto<>();
        response.setStatus(ResponseDto.Status.SUCCESS);
        response.setData(data);
        response.setMessage(data.size() + " effective permission(s)");
        return ResponseEntity.ok(response);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Users
    // ══════════════════════════════════════════════════════════════════════════

    @Operation(summary = "List all users (paginated)", description = "Returns all users with their group, role and permission context. "
            + "Passwords and security-sensitive fields are never included. "
            + "Supports sorting by any User field via the `sort` parameter.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User page returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient authority", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/users")
    @PreAuthorize("hasAuthority('USER_GROUPS_MANAGE')")
    public ResponseEntity<ResponseDto<PageDto<UserDto>>> getUsers(
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<UserDto> page = authService.getUsers(pageable);
        PageDto<UserDto> data = PageDto.from(page);

        ResponseDto<PageDto<UserDto>> response = new ResponseDto<>();
        response.setStatus(ResponseDto.Status.SUCCESS);
        response.setData(data);
        response.setMessage(data.getTotalElements() + " user(s) found");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get user by ID", description = "Returns a single user with their groups, roles, and effective permissions.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient authority", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('USER_GROUPS_MANAGE')")
    public ResponseEntity<ResponseDto<UserDto>> getUser(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        UserDto data = authService.getUserById(userId);

        ResponseDto<UserDto> response = new ResponseDto<>();
        response.setStatus(ResponseDto.Status.SUCCESS);
        response.setData(data);
        return ResponseEntity.ok(response);
    }
}
