package com.shop.auth.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.shop.auth.dto.UserGroupDto;
import com.shop.auth.entity.BankingRole;
import com.shop.auth.entity.Permission;
import com.shop.auth.entity.User;
import com.shop.auth.entity.UserGroup;
import com.shop.auth.exception.ResourceNotFoundException;
import com.shop.auth.repository.BankingRoleRepository;
import com.shop.auth.repository.UserGroupRepository;
import com.shop.auth.repository.UserRepository;

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

    @Mock private UserGroupRepository   userGroupRepository;
    @Mock private BankingRoleRepository bankingRoleRepository;
    @Mock private UserRepository        userRepository;

    @InjectMocks private UserGroupServiceImpl service;

    private UserGroup retailGroup;
    private BankingRole basicRole;
    private Permission  accountView;
    private User        activeUser;

    @BeforeEach
    void setUp() {
        accountView = new Permission();
        accountView.setId(1L);
        accountView.setCode("ACCOUNT_VIEW");
        accountView.setCategory("ACCOUNT");

        basicRole = new BankingRole();
        basicRole.setId(5L);
        basicRole.setName("ROLE_CUSTOMER_BASIC");
        basicRole.setPermissions(new HashSet<>(Set.of(accountView)));

        retailGroup = new UserGroup();
        retailGroup.setId(1L);
        retailGroup.setName("RETAIL_CUSTOMER");
        retailGroup.setType("CUSTOMER");
        retailGroup.setRoles(new HashSet<>(Set.of(basicRole)));

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
            when(userGroupRepository.findAll()).thenReturn(List.of(retailGroup));

            List<UserGroupDto> result = service.listAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("RETAIL_CUSTOMER");
            assertThat(result.get(0).getRoles()).hasSize(1);
            assertThat(result.get(0).getRoles().get(0).getName()).isEqualTo("ROLE_CUSTOMER_BASIC");
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
            when(userGroupRepository.findById(1L)).thenReturn(Optional.of(retailGroup));

            UserGroupDto result = service.getById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("RETAIL_CUSTOMER");
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
            BankingRole newRole = new BankingRole();
            newRole.setId(9L);
            newRole.setName("ROLE_LOAN_PROCESSOR");
            newRole.setPermissions(new HashSet<>());
            retailGroup.setRoles(new HashSet<>());

            when(userGroupRepository.findById(1L)).thenReturn(Optional.of(retailGroup));
            when(bankingRoleRepository.findById(9L)).thenReturn(Optional.of(newRole));
            when(userGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserGroupDto result = service.assignRoleToGroup(1L, 9L);

            assertThat(result.getRoles()).hasSize(1);
            verify(userGroupRepository).save(retailGroup);
        }

        @Test
        @DisplayName("Is idempotent — no save when role already assigned")
        void shouldBeIdempotent() {
            when(userGroupRepository.findById(1L)).thenReturn(Optional.of(retailGroup));
            when(bankingRoleRepository.findById(5L)).thenReturn(Optional.of(basicRole));

            service.assignRoleToGroup(1L, 5L);  // basicRole already in retailGroup

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
            activeUser.getGroups().add(retailGroup);
            when(userRepository.findById(42L)).thenReturn(Optional.of(activeUser));

            List<UserGroupDto> result = service.getUserGroups(42L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("RETAIL_CUSTOMER");
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
            when(userGroupRepository.findById(1L)).thenReturn(Optional.of(retailGroup));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.addUserToGroup(42L, 1L);

            assertThat(activeUser.getGroups()).contains(retailGroup);
            verify(userRepository).save(activeUser);
        }

        @Test
        @DisplayName("Is idempotent — no save when user already in group")
        void shouldBeIdempotent() {
            activeUser.getGroups().add(retailGroup);
            when(userRepository.findById(42L)).thenReturn(Optional.of(activeUser));
            when(userGroupRepository.findById(1L)).thenReturn(Optional.of(retailGroup));

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
            activeUser.getGroups().add(retailGroup);  // retailGroup → basicRole → ACCOUNT_VIEW
            when(userRepository.findById(42L)).thenReturn(Optional.of(activeUser));

            Set<String> result = service.getEffectivePermissions(42L);

            assertThat(result).containsExactly("ACCOUNT_VIEW");
        }

        @Test
        @DisplayName("Returns permissions from direct role assignments")
        void shouldReturnDirectRolePermissions() {
            Permission txnView = new Permission();
            txnView.setId(2L);
            txnView.setCode("TRANSACTION_VIEW");
            BankingRole directRole = new BankingRole();
            directRole.setId(8L);
            directRole.setPermissions(new HashSet<>(Set.of(txnView)));
            activeUser.getDirectRoles().add(directRole);

            when(userRepository.findById(42L)).thenReturn(Optional.of(activeUser));

            Set<String> result = service.getEffectivePermissions(42L);

            assertThat(result).containsExactly("TRANSACTION_VIEW");
        }

        @Test
        @DisplayName("Returns union of group and direct-role permissions without duplicates")
        void shouldReturnUnionWithoutDuplicates() {
            // Both group and direct role have ACCOUNT_VIEW — should appear once
            activeUser.getGroups().add(retailGroup);

            BankingRole directRole = new BankingRole();
            directRole.setId(8L);
            directRole.setPermissions(new HashSet<>(Set.of(accountView))); // same permission
            activeUser.getDirectRoles().add(directRole);

            when(userRepository.findById(42L)).thenReturn(Optional.of(activeUser));

            Set<String> result = service.getEffectivePermissions(42L);

            assertThat(result).hasSize(1).containsExactly("ACCOUNT_VIEW");
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
