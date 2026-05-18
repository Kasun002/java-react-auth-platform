package com.org.auth.service;

import java.util.List;
import java.util.Set;

import com.org.auth.dto.CreateGroupRequestDto;
import com.org.auth.dto.UpdateGroupRequestDto;
import com.org.auth.dto.UserGroupDto;
import com.org.auth.exception.ConflictException;
import com.org.auth.exception.ResourceNotFoundException;

public interface UserGroupService {

    /** Returns all groups with their assigned roles and permissions. */
    List<UserGroupDto> listAll();

    /**
     * Creates a new user group.
     *
     * @throws ConflictException if a group with the same name already exists
     */
    UserGroupDto create(CreateGroupRequestDto request);

    /**
     * Updates an existing group.
     *
     * @throws ResourceNotFoundException if no group exists with the given id
     * @throws ConflictException         if the new name is already taken by another group
     */
    UserGroupDto update(Long groupId, UpdateGroupRequestDto request);

    /**
     * Deletes a group.
     *
     * @throws ResourceNotFoundException if no group exists with the given id
     * @throws ConflictException         if the group still has members
     */
    void delete(Long groupId);

    /**
     * Returns a single group by ID.
     *
     * @throws ResourceNotFoundException if no group exists with the given id
     */
    UserGroupDto getById(Long groupId);

    /**
     * Assigns a role to a group. Idempotent — no-op if already assigned.
     *
     * @throws ResourceNotFoundException if group or role does not exist
     */
    UserGroupDto assignRoleToGroup(Long groupId, Long roleId);

    /**
     * Removes a role from a group. No-op if the role is not assigned.
     *
     * @throws ResourceNotFoundException if group or role does not exist
     */
    void removeRoleFromGroup(Long groupId, Long roleId);

    /**
     * Returns all groups the user belongs to.
     *
     * @throws ResourceNotFoundException if no user exists with the given id
     */
    List<UserGroupDto> getUserGroups(Long userId);

    /**
     * Adds a user to a group. Idempotent — no-op if already a member.
     *
     * @throws ResourceNotFoundException if user or group does not exist
     */
    void addUserToGroup(Long userId, Long groupId);

    /**
     * Removes a user from a group. No-op if the user is not a member.
     *
     * @throws ResourceNotFoundException if user or group does not exist
     */
    void removeUserFromGroup(Long userId, Long groupId);

    /**
     * Returns the effective permission codes for a user — union of permissions
     * from all group-assigned roles and directly assigned roles.
     *
     * @throws ResourceNotFoundException if no user exists with the given id
     */
    Set<String> getEffectivePermissions(Long userId);
}
