package com.org.auth.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.org.auth.dto.UserGroupDto;
import com.org.auth.entity.Permission;
import com.org.auth.entity.Role;
import com.org.auth.entity.User;
import com.org.auth.entity.UserGroup;
import com.org.auth.exception.ResourceNotFoundException;
import com.org.auth.repository.RoleRepository;
import com.org.auth.repository.UserGroupRepository;
import com.org.auth.repository.UserRepository;

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

@DisplayName("UserGroupServiceImpl")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class UserGroupServiceImplTest {

    @Mock private UserGroupRepository userGroupRepository;
    @Mock private RoleRepository      roleRepository;
    @Mock private UserRepository      userRepository;

    @InjectMocks private UserGroupServiceImpl service;

    private UserGroup adminGroup;
    private Role      managerRole;
    private Permission userView;
    private User      activeUser;

    @BeforeEach
    void setUp() {
        userView = new Permission();
        userView.setId(1L);
        userView.setCode("USER_VIEW");
        userView.setCategory("USER");

        managerRole = new Role();
        managerRole.setId(5L);
        managerRole.setName("ROLE_MANAGER");
        managerRole.setPermissions(new HashSet<>(Set.of(userView)));

        adminGroup = new UserGroup();
        adminGroup.setId(1L);
        adminGroup.setName("SYSTEM_ADMIN");
        adminGroup.setType("ADMIN");
        adminGroup.setRoles(new HashSet<>(Set.of(managerRole)));

        activeUser = new User();
        activeUser.setId(42L);
        activeUser.setName("John Doe");
        activeUser.setEmail("john@example.com");
        activeUser.setGroups(new HashSet<>());
        activeUser.setDirectRoles(new HashSet<>());
    }

    // ── listAll ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("listAll")
    class ListAll {

        @Test
        @DisplayName("Returns mapped DTOs including nested roles and permissions")
        void shouldReturnMappedDtos() {
            when(userGroupRepository.findAll()).thenReturn(List.of(adminGroup));

            List<UserGroupDto> result = service.listAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("SYSTEM_ADMIN");
            assertThat(result.get(0).getRoles()).hasSize(1);
            assertThat(result.get(0).getRoles().get(0).getName()).isEqualTo("ROLE_MANAGER");
            assertThat(result.get(0).getRoles().get(0).getPermissions()).hasSize(1);
        }

        @Test
        @DisplayName("Returns empty list when no groups exist")
        void shouldReturnEmptyList() {
            when(userGroupRepository.findAll()).thenReturn(List.of());
            assertThat(service.listAll()).isEmpty();
        }
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("Returns DTO when group exists")
        void shouldReturnDto() {
            when(userGroupRepository.findById(1L)).thenReturn(Optional.of(adminGroup));

            UserGroupDto result = service.getById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("SYSTEM_ADMIN");
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when group not found")
        void shouldThrowWhenNotFound() {
            when(userGroupRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // ── assignRoleToGroup ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("assignRoleToGroup")
    class AssignRoleToGroup {

        @Test
        @DisplayName("Adds role to group when not already assigned")
        void shouldAddRole() {
            Role newRole = new Role();
            newRole.setId(9L);
            newRole.setName("ROLE_VIEWER");
            newRole.setPermissions(new HashSet<>());
            adminGroup.setRoles(new HashSet<>());

            when(userGroupRepository.findById(1L)).thenReturn(Optional.of(adminGroup));
            when(roleRepository.findById(9L)).thenReturn(Optional.of(newRole));
            when(userGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserGroupDto result = service.assignRoleToGroup(1L, 9L);

            assertThat(result.getRoles()).hasSize(1);
            verify(userGroupRepository).save(adminGroup);
        }

        @Test
        @DisplayName("Is idempotent — no save when role already assigned")
        void shouldBeIdempotent() {
            when(userGroupRepository.findById(1L)).thenReturn(Optional.of(adminGroup));
            when(roleRepository.findById(5L)).thenReturn(Optional.of(managerRole));

            service.assignRoleToGroup(1L, 5L);  // managerRole already in adminGroup

            verify(userGroupRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when group not found")
        void shouldThrowWhenGroupNotFound() {
            when(userGroupRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.assignRoleToGroup(99L, 5L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── getUserGroups ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserGroups")
    class GetUserGroups {

        @Test
        @DisplayName("Returns user's group list")
        void shouldReturnUserGroups() {
            activeUser.getGroups().add(adminGroup);
            when(userRepository.findById(42L)).thenReturn(Optional.of(activeUser));

            List<UserGroupDto> result = service.getUserGroups(42L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("SYSTEM_ADMIN");
        }

        @Test
        @DisplayName("Returns empty list when user has no groups")
        void shouldReturnEmptyWhenNoGroups() {
            when(userRepository.findById(42L)).thenReturn(Optional.of(activeUser));
            assertThat(service.getUserGroups(42L)).isEmpty();
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when user not found")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.getUserGroups(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── addUserToGroup ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addUserToGroup")
    class AddUserToGroup {

        @Test
        @DisplayName("Adds user to group and saves")
        void shouldAddUserToGroup() {
            when(userRepository.findById(42L)).thenReturn(Optional.of(activeUser));
            when(userGroupRepository.findById(1L)).thenReturn(Optional.of(adminGroup));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.addUserToGroup(42L, 1L);

            assertThat(activeUser.getGroups()).contains(adminGroup);
            verify(userRepository).save(activeUser);
        }

        @Test
        @DisplayName("Is idempotent — no save when user already in group")
        void shouldBeIdempotent() {
            activeUser.getGroups().add(adminGroup);
            when(userRepository.findById(42L)).thenReturn(Optional.of(activeUser));
            when(userGroupRepository.findById(1L)).thenReturn(Optional.of(adminGroup));

            service.addUserToGroup(42L, 1L);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when user not found")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.addUserToGroup(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── getEffectivePermissions ───────────────────────────────────────────────

    @Nested
    @DisplayName("getEffectivePermissions")
    class GetEffectivePermissions {

        @Test
        @DisplayName("Returns permissions from group-assigned roles")
        void shouldReturnGroupPermissions() {
            activeUser.getGroups().add(adminGroup);  // adminGroup → managerRole → USER_VIEW
            when(userRepository.findById(42L)).thenReturn(Optional.of(activeUser));

            Set<String> result = service.getEffectivePermissions(42L);

            assertThat(result).containsExactly("USER_VIEW");
        }

        @Test
        @DisplayName("Returns permissions from direct role assignments")
        void shouldReturnDirectRolePermissions() {
            Permission groupManage = new Permission();
            groupManage.setId(2L);
            groupManage.setCode("GROUP_MANAGE");
            Role directRole = new Role();
            directRole.setId(8L);
            directRole.setPermissions(new HashSet<>(Set.of(groupManage)));
            activeUser.getDirectRoles().add(directRole);

            when(userRepository.findById(42L)).thenReturn(Optional.of(activeUser));

            Set<String> result = service.getEffectivePermissions(42L);

            assertThat(result).containsExactly("GROUP_MANAGE");
        }

        @Test
        @DisplayName("Returns union of group and direct-role permissions without duplicates")
        void shouldReturnUnionWithoutDuplicates() {
            activeUser.getGroups().add(adminGroup);

            Role directRole = new Role();
            directRole.setId(8L);
            directRole.setPermissions(new HashSet<>(Set.of(userView))); // same permission
            activeUser.getDirectRoles().add(directRole);

            when(userRepository.findById(42L)).thenReturn(Optional.of(activeUser));

            Set<String> result = service.getEffectivePermissions(42L);

            assertThat(result).hasSize(1).containsExactly("USER_VIEW");
        }

        @Test
        @DisplayName("Returns empty set when user has no groups or roles")
        void shouldReturnEmptyWhenNoRoles() {
            when(userRepository.findById(42L)).thenReturn(Optional.of(activeUser));
            assertThat(service.getEffectivePermissions(42L)).isEmpty();
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when user not found")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.getEffectivePermissions(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
