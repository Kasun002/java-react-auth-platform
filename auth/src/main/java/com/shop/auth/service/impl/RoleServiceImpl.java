package com.shop.auth.service.impl;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.shop.auth.dto.PermissionDto;
import com.shop.auth.dto.RoleDto;
import com.shop.auth.entity.Permission;
import com.shop.auth.entity.Role;
import com.shop.auth.exception.ResourceNotFoundException;
import com.shop.auth.repository.PermissionRepository;
import com.shop.auth.repository.RoleRepository;
import com.shop.auth.service.RoleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleServiceImpl implements RoleService {

    private final RoleRepository       roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    public List<RoleDto> listAll() {
        log.debug("Listing all roles");
        return roleRepository.findAll().stream()
                .sorted(Comparator.comparing(Role::getName))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public RoleDto getById(Long roleId) {
        log.debug("Fetching role id=[{}]", roleId);
        return roleRepository.findById(roleId)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));
    }

    @Override
    @Transactional
    public RoleDto assignPermission(Long roleId, Long permissionId) {
        log.debug("Assigning permission id=[{}] to role id=[{}]", permissionId, roleId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", permissionId));

        boolean alreadyAssigned = role.getPermissions().stream()
                .anyMatch(p -> p.getId().equals(permissionId));

        if (!alreadyAssigned) {
            role.getPermissions().add(permission);
            roleRepository.save(role);
            log.info("Permission [{}] assigned to role [{}]", permission.getCode(), role.getName());
        } else {
            log.debug("Permission [{}] already assigned to role [{}] — no-op", permission.getCode(), role.getName());
        }

        return toDto(role);
    }

    @Override
    @Transactional
    public void removePermission(Long roleId, Long permissionId) {
        log.debug("Removing permission id=[{}] from role id=[{}]", permissionId, roleId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));
        if (!permissionRepository.existsById(permissionId)) {
            throw new ResourceNotFoundException("Permission", permissionId);
        }

        boolean removed = role.getPermissions().removeIf(p -> p.getId().equals(permissionId));
        if (removed) {
            roleRepository.save(role);
            log.info("Permission id=[{}] removed from role [{}]", permissionId, role.getName());
        } else {
            log.debug("Permission id=[{}] was not assigned to role [{}] — no-op", permissionId, role.getName());
        }
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    RoleDto toDto(Role role) {
        RoleDto dto = new RoleDto();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());
        dto.setPermissions(role.getPermissions().stream()
                .sorted(Comparator.comparing(Permission::getCode))
                .map(this::toPermissionDto)
                .collect(Collectors.toList()));
        return dto;
    }

    private PermissionDto toPermissionDto(Permission permission) {
        PermissionDto dto = new PermissionDto();
        dto.setId(permission.getId());
        dto.setCode(permission.getCode());
        dto.setDescription(permission.getDescription());
        dto.setCategory(permission.getCategory());
        return dto;
    }
}
