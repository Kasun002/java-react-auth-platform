package com.shop.auth.service.impl;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.shop.auth.dto.PermissionDto;
import com.shop.auth.entity.Permission;
import com.shop.auth.repository.PermissionRepository;
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

    @Override
    public List<PermissionDto> listAll() {
        log.debug("Listing all permissions");
        return permissionRepository.findAll().stream()
                .sorted(Comparator.comparing(Permission::getCategory)
                        .thenComparing(Permission::getCode))
                .map(this::toDto)
                .collect(Collectors.toList());
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
