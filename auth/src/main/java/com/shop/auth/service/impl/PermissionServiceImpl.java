package com.shop.auth.service.impl;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.shop.auth.dto.CreatePermissionRequestDto;
import com.shop.auth.dto.PermissionDto;
import com.shop.auth.dto.UpdatePermissionRequestDto;
import com.shop.auth.entity.Permission;
import com.shop.auth.exception.ConflictException;
import com.shop.auth.exception.ResourceNotFoundException;
import com.shop.auth.repository.PermissionRepository;
import com.shop.auth.repository.RoleRepository;
import com.shop.auth.service.PermissionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository       roleRepository;

    @Override
    public List<PermissionDto> listAll() {
        log.debug("Listing all permissions");
        return permissionRepository.findAll().stream()
                .sorted(Comparator.comparing(Permission::getCategory)
                        .thenComparing(Permission::getCode))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PermissionDto create(CreatePermissionRequestDto request) {
        String code = request.getCode().trim().toUpperCase();
        log.debug("Creating permission code=[{}]", code);

        if (permissionRepository.findByCode(code).isPresent()) {
            throw new ConflictException("Permission with code '" + code + "' already exists");
        }

        Permission permission = new Permission();
        permission.setCode(code);
        permission.setCategory(request.getCategory().trim().toUpperCase());
        permission.setDescription(request.getDescription());

        Permission saved = permissionRepository.save(permission);
        log.info("Permission created: id=[{}] code=[{}]", saved.getId(), saved.getCode());
        return toDto(saved);
    }

    @Override
    @Transactional
    public PermissionDto update(Long id, UpdatePermissionRequestDto request) {
        String code = request.getCode().trim().toUpperCase();
        log.debug("Updating permission id=[{}] code=[{}]", id, code);

        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", id));

        permissionRepository.findByCode(code)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("Permission with code '" + code + "' already exists");
                });

        permission.setCode(code);
        permission.setCategory(request.getCategory().trim().toUpperCase());
        permission.setDescription(request.getDescription());

        Permission saved = permissionRepository.save(permission);
        log.info("Permission updated: id=[{}] code=[{}]", saved.getId(), saved.getCode());
        return toDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.debug("Deleting permission id=[{}]", id);

        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", id));

        if (roleRepository.existsByPermissionsId(id)) {
            throw new ConflictException(
                    "Permission '" + permission.getCode() + "' is still assigned to one or more roles and cannot be deleted");
        }

        permissionRepository.delete(permission);
        log.info("Permission deleted: id=[{}] code=[{}]", id, permission.getCode());
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private PermissionDto toDto(Permission permission) {
        PermissionDto dto = new PermissionDto();
        dto.setId(permission.getId());
        dto.setCode(permission.getCode());
        dto.setDescription(permission.getDescription());
        dto.setCategory(permission.getCategory());
        return dto;
    }
}
