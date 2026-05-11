package com.shop.auth.service;

import java.util.List;

import com.shop.auth.dto.CreatePermissionRequestDto;
import com.shop.auth.dto.PermissionDto;
import com.shop.auth.dto.UpdatePermissionRequestDto;
import com.shop.auth.exception.ConflictException;
import com.shop.auth.exception.ResourceNotFoundException;

public interface PermissionService {

    /** Returns all permissions ordered by category and code. */
    List<PermissionDto> listAll();

    /**
     * Creates a new permission.
     *
     * @throws ConflictException if a permission with the same code already exists
     */
    PermissionDto create(CreatePermissionRequestDto request);

    /**
     * Updates an existing permission.
     *
     * @throws ResourceNotFoundException if no permission exists with the given id
     * @throws ConflictException         if the new code is already taken by another permission
     */
    PermissionDto update(Long id, UpdatePermissionRequestDto request);

    /**
     * Deletes a permission.
     *
     * @throws ResourceNotFoundException if no permission exists with the given id
     * @throws ConflictException         if the permission is still assigned to one or more roles
     */
    void delete(Long id);
}
