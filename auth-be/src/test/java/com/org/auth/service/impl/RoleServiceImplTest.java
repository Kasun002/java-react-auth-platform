package com.org.auth.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.org.auth.dto.RoleDto;
import com.org.auth.entity.Permission;
import com.org.auth.entity.Role;
import com.org.auth.exception.ResourceNotFoundException;
import com.org.auth.repository.PermissionRepository;
import com.org.auth.repository.RoleRepository;
import com.org.auth.service.AuditHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("RoleServiceImpl")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class RoleServiceImplTest {

    @Mock private RoleRepository       roleRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private AuditHelper          auditHelper;

    @InjectMocks private RoleServiceImpl service;

    private Role       managerRole;
    private Permission userView;

    @BeforeEach
    void setUp() {
        userView = new Permission();
        userView.setId(1L);
        userView.setCode("USER_VIEW");
        userView.setCategory("USER");

        managerRole = new Role();
        managerRole.setId(10L);
        managerRole.setName("ROLE_MANAGER");
        managerRole.setDescription("Department manager");
        managerRole.setPermissions(new HashSet<>(Set.of(userView)));
    }

    // ── listAll ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("listAll")
    class ListAll {

        @Test
        @DisplayName("Returns mapped DTOs for all roles")
        void shouldReturnAllRoles() {
            when(roleRepository.findAll()).thenReturn(List.of(managerRole));

            List<RoleDto> result = service.listAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("ROLE_MANAGER");
            assertThat(result.get(0).getPermissions()).hasSize(1);
            assertThat(result.get(0).getPermissions().get(0).getCode()).isEqualTo("USER_VIEW");
        }

        @Test
        @DisplayName("Returns empty list when no roles exist")
        void shouldReturnEmptyListWhenNoRoles() {
            when(roleRepository.findAll()).thenReturn(List.of());
            assertThat(service.listAll()).isEmpty();
        }
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("Returns DTO when role exists")
        void shouldReturnDtoWhenFound() {
            when(roleRepository.findById(10L)).thenReturn(Optional.of(managerRole));

            RoleDto result = service.getById(10L);

            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getName()).isEqualTo("ROLE_MANAGER");
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when role does not exist")
        void shouldThrowWhenNotFound() {
            when(roleRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // ── assignPermission ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("assignPermission")
    class AssignPermission {

        @Test
        @DisplayName("Adds permission to role when not already assigned")
        void shouldAddPermission() {
            Permission groupManage = new Permission();
            groupManage.setId(2L);
            groupManage.setCode("GROUP_MANAGE");
            groupManage.setCategory("ADMIN");

            managerRole.setPermissions(new HashSet<>());
            when(roleRepository.findById(10L)).thenReturn(Optional.of(managerRole));
            when(permissionRepository.findById(2L)).thenReturn(Optional.of(groupManage));
            when(roleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            RoleDto result = service.assignPermission(10L, 2L);

            assertThat(result.getPermissions()).hasSize(1);
            assertThat(result.getPermissions().get(0).getCode()).isEqualTo("GROUP_MANAGE");
            verify(roleRepository).save(managerRole);
        }

        @Test
        @DisplayName("Is idempotent — does not save when permission already assigned")
        void shouldBeIdempotent() {
            when(roleRepository.findById(10L)).thenReturn(Optional.of(managerRole));
            when(permissionRepository.findById(1L)).thenReturn(Optional.of(userView));

            service.assignPermission(10L, 1L);  // userView already in managerRole

            verify(roleRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when role does not exist")
        void shouldThrowWhenRoleNotFound() {
            when(roleRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.assignPermission(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when permission does not exist")
        void shouldThrowWhenPermissionNotFound() {
            when(roleRepository.findById(10L)).thenReturn(Optional.of(managerRole));
            when(permissionRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.assignPermission(10L, 99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // ── removePermission ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("removePermission")
    class RemovePermission {

        @Test
        @DisplayName("Removes permission from role when assigned")
        void shouldRemovePermission() {
            when(roleRepository.findById(10L)).thenReturn(Optional.of(managerRole));
            when(permissionRepository.existsById(1L)).thenReturn(true);
            when(roleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.removePermission(10L, 1L);

            assertThat(managerRole.getPermissions()).isEmpty();
            verify(roleRepository).save(managerRole);
        }

        @Test
        @DisplayName("Is idempotent — does not save when permission not assigned")
        void shouldBeIdempotentWhenNotAssigned() {
            managerRole.setPermissions(new HashSet<>());
            when(roleRepository.findById(10L)).thenReturn(Optional.of(managerRole));
            when(permissionRepository.existsById(1L)).thenReturn(true);

            service.removePermission(10L, 1L);

            verify(roleRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when role not found")
        void shouldThrowWhenRoleNotFound() {
            when(roleRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.removePermission(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when permission not found")
        void shouldThrowWhenPermissionNotFound() {
            when(roleRepository.findById(10L)).thenReturn(Optional.of(managerRole));
            when(permissionRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> service.removePermission(10L, 99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
