package com.shop.auth.service;

import java.util.List;

import com.shop.auth.dto.BankingRoleDto;
import com.shop.auth.exception.ResourceNotFoundException;

public interface BankingRoleService {

    /** Returns all roles with their assigned permissions. */
    List<BankingRoleDto> listAll();

    /**
     * Returns a single role by ID.
     *
     * @throws ResourceNotFoundException if no role exists with the given id
     */
    BankingRoleDto getById(Long roleId);

    /**
     * Assigns a permission to a role. Idempotent — no-op if already assigned.
     *
     * @throws ResourceNotFoundException if role or permission does not exist
     */
    BankingRoleDto assignPermission(Long roleId, Long permissionId);

    /**
     * Removes a permission from a role. No-op if the permission is not assigned.
     *
     * @throws ResourceNotFoundException if role or permission does not exist
     */
    void removePermission(Long roleId, Long permissionId);
}
