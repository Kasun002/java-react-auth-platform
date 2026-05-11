package com.shop.auth.service.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.auth.dto.CreateGroupRequestDto;
import com.shop.auth.dto.PermissionDto;
import com.shop.auth.dto.RoleDto;
import com.shop.auth.dto.UpdateGroupRequestDto;
import com.shop.auth.dto.UserGroupDto;
import com.shop.auth.entity.Permission;
import com.shop.auth.entity.Role;
import com.shop.auth.entity.User;
import com.shop.auth.entity.UserGroup;
import com.shop.auth.exception.ConflictException;
import com.shop.auth.exception.ResourceNotFoundException;
import com.shop.auth.repository.RoleRepository;
import com.shop.auth.repository.UserGroupRepository;
import com.shop.auth.repository.UserRepository;
import com.shop.auth.service.UserGroupService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserGroupServiceImpl implements UserGroupService {

    private final UserGroupRepository userGroupRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @Override
    public List<UserGroupDto> listAll() {
        log.debug("Listing all user groups");
        return userGroupRepository.findAll().stream()
                .sorted(Comparator.comparing(UserGroup::getName))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserGroupDto getById(Long groupId) {
        log.debug("Fetching user group id=[{}]", groupId);
        return userGroupRepository.findById(groupId)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Group", groupId));
    }

    @Override
    @Transactional
    public UserGroupDto create(CreateGroupRequestDto request) {
        String name = request.getName().trim();
        log.debug("Creating group name=[{}]", name);

        if (userGroupRepository.existsByName(name)) {
            throw new ConflictException("Group with name '" + name + "' already exists");
        }

        UserGroup group = new UserGroup();
        group.setName(name);
        group.setType(request.getType().trim());
        group.setDescription(request.getDescription());

        UserGroup saved = userGroupRepository.save(group);
        log.info("Group created: id=[{}] name=[{}]", saved.getId(), saved.getName());
        return toDto(saved);
    }

    @Override
    @Transactional
    public UserGroupDto update(Long groupId, UpdateGroupRequestDto request) {
        String name = request.getName().trim();
        log.debug("Updating group id=[{}] name=[{}]", groupId, name);

        UserGroup group = userGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", groupId));

        userGroupRepository.findByName(name)
                .filter(existing -> !existing.getId().equals(groupId))
                .ifPresent(existing -> {
                    throw new ConflictException("Group with name '" + name + "' already exists");
                });

        group.setName(name);
        group.setType(request.getType().trim());
        group.setDescription(request.getDescription());

        UserGroup saved = userGroupRepository.save(group);
        log.info("Group updated: id=[{}] name=[{}]", saved.getId(), saved.getName());
        return toDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long groupId) {
        log.debug("Deleting group id=[{}]", groupId);

        UserGroup group = userGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", groupId));

        if (userRepository.existsByGroupsId(groupId)) {
            throw new ConflictException(
                    "Group '" + group.getName() + "' still has members and cannot be deleted");
        }

        userGroupRepository.delete(group);
        log.info("Group deleted: id=[{}] name=[{}]", groupId, group.getName());
    }

    @Override
    @Transactional
    public UserGroupDto assignRoleToGroup(Long groupId, Long roleId) {
        log.debug("Assigning role id=[{}] to group id=[{}]", roleId, groupId);

        UserGroup group = userGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", groupId));
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));

        boolean alreadyAssigned = group.getRoles().stream()
                .anyMatch(r -> r.getId().equals(roleId));

        if (!alreadyAssigned) {
            group.getRoles().add(role);
            userGroupRepository.save(group);
            log.info("Role [{}] assigned to group [{}]", role.getName(), group.getName());
        } else {
            log.debug("Role [{}] already in group [{}] — no-op", role.getName(), group.getName());
        }

        return toDto(group);
    }

    @Override
    @Transactional
    public void removeRoleFromGroup(Long groupId, Long roleId) {
        log.debug("Removing role id=[{}] from group id=[{}]", roleId, groupId);

        UserGroup group = userGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", groupId));
        if (!roleRepository.existsById(roleId)) {
            throw new ResourceNotFoundException("Role", roleId);
        }

        boolean removed = group.getRoles().removeIf(r -> r.getId().equals(roleId));
        if (removed) {
            userGroupRepository.save(group);
            log.info("Role id=[{}] removed from group [{}]", roleId, group.getName());
        } else {
            log.debug("Role id=[{}] was not in group [{}] — no-op", roleId, group.getName());
        }
    }

    @Override
    public List<UserGroupDto> getUserGroups(Long userId) {
        log.debug("Fetching groups for user id=[{}]", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return user.getGroups().stream()
                .sorted(Comparator.comparing(UserGroup::getName))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addUserToGroup(Long userId, Long groupId) {
        log.debug("Adding user id=[{}] to group id=[{}]", userId, groupId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        UserGroup group = userGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", groupId));

        boolean alreadyMember = user.getGroups().stream()
                .anyMatch(g -> g.getId().equals(groupId));

        if (!alreadyMember) {
            user.getGroups().add(group);
            userRepository.save(user);
            log.info("User id=[{}] added to group [{}]", userId, group.getName());
        } else {
            log.debug("User id=[{}] already in group [{}] — no-op", userId, group.getName());
        }
    }

    @Override
    @Transactional
    public void removeUserFromGroup(Long userId, Long groupId) {
        log.debug("Removing user id=[{}] from group id=[{}]", userId, groupId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (!userGroupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException("Group", groupId);
        }

        boolean removed = user.getGroups().removeIf(g -> g.getId().equals(groupId));
        if (removed) {
            userRepository.save(user);
            log.info("User id=[{}] removed from group id=[{}]", userId, groupId);
        } else {
            log.debug("User id=[{}] was not in group id=[{}] — no-op", userId, groupId);
        }
    }

    @Override
    public Set<String> getEffectivePermissions(Long userId) {
        log.debug("Computing effective permissions for user id=[{}]", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Set<String> permissions = new LinkedHashSet<>();

        // Via group memberships
        for (UserGroup group : user.getGroups()) {
            for (Role role : group.getRoles()) {
                role.getPermissions().forEach(p -> permissions.add(p.getCode()));
            }
        }
        // Via direct role assignments
        for (Role role : user.getDirectRoles()) {
            role.getPermissions().forEach(p -> permissions.add(p.getCode()));
        }

        log.debug("Effective permissions for user id=[{}]: count=[{}]", userId, permissions.size());
        return Collections.unmodifiableSet(permissions);
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private UserGroupDto toDto(UserGroup group) {
        UserGroupDto dto = new UserGroupDto();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setDescription(group.getDescription());
        dto.setType(group.getType());
        dto.setRoles(group.getRoles().stream()
                .sorted(Comparator.comparing(Role::getName))
                .map(this::toRoleDto)
                .collect(Collectors.toList()));
        return dto;
    }

    private RoleDto toRoleDto(Role role) {
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
