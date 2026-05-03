package com.shop.auth.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.shop.auth.dto.BankingRoleDto;
import com.shop.auth.entity.BankingRole;
import com.shop.auth.entity.Permission;
import com.shop.auth.exception.ResourceNotFoundException;
import com.shop.auth.repository.BankingRoleRepository;
import com.shop.auth.repository.PermissionRepository;

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

@DisplayName("BankingRoleServiceImpl")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class BankingRoleServiceImplTest {

    @Mock private BankingRoleRepository bankingRoleRepository;
    @Mock private PermissionRepository  permissionRepository;

    @InjectMocks private BankingRoleServiceImpl service;

    private BankingRole tellerRole;
    private Permission  accountView;

    @BeforeEach
    void setUp() {
        accountView = new Permission();
        accountView.setId(1L);
        accountView.setCode("ACCOUNT_VIEW");
        accountView.setCategory("ACCOUNT");

        tellerRole = new BankingRole();
        tellerRole.setId(10L);
        tellerRole.setName("ROLE_TELLER");
        tellerRole.setDescription("Front-line branch");
        tellerRole.setPermissions(new HashSet<>(Set.of(accountView)));
    }

    // ── listAll ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("listAll")
    class ListAll {

        @Test
        @DisplayName("Returns mapped DTOs for all roles")
        void shouldReturnAllRoles() {
            when(bankingRoleRepository.findAll()).thenReturn(List.of(tellerRole));

            List<BankingRoleDto> result = service.listAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("ROLE_TELLER");
            assertThat(result.get(0).getPermissions()).hasSize(1);
            assertThat(result.get(0).getPermissions().get(0).getCode()).isEqualTo("ACCOUNT_VIEW");
        }

        @Test
        @DisplayName("Returns empty list when no roles exist")
        void shouldReturnEmptyListWhenNoRoles() {
            when(bankingRoleRepository.findAll()).thenReturn(List.of());
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
            when(bankingRoleRepository.findById(10L)).thenReturn(Optional.of(tellerRole));

            BankingRoleDto result = service.getById(10L);

            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getName()).isEqualTo("ROLE_TELLER");
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when role does not exist")
        void shouldThrowWhenNotFound() {
            when(bankingRoleRepository.findById(99L)).thenReturn(Optional.empty());

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
            Permission txnView = new Permission();
            txnView.setId(2L);
            txnView.setCode("TRANSACTION_VIEW");
            txnView.setCategory("TRANSACTION");

            tellerRole.setPermissions(new HashSet<>());   // start with empty set
            when(bankingRoleRepository.findById(10L)).thenReturn(Optional.of(tellerRole));
            when(permissionRepository.findById(2L)).thenReturn(Optional.of(txnView));
            when(bankingRoleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BankingRoleDto result = service.assignPermission(10L, 2L);

            assertThat(result.getPermissions()).hasSize(1);
            assertThat(result.getPermissions().get(0).getCode()).isEqualTo("TRANSACTION_VIEW");
            verify(bankingRoleRepository).save(tellerRole);
        }

        @Test
        @DisplayName("Is idempotent — does not save when permission already assigned")
        void shouldBeIdempotent() {
            when(bankingRoleRepository.findById(10L)).thenReturn(Optional.of(tellerRole));
            when(permissionRepository.findById(1L)).thenReturn(Optional.of(accountView));

            service.assignPermission(10L, 1L);  // accountView already in tellerRole

            verify(bankingRoleRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when role does not exist")
        void shouldThrowWhenRoleNotFound() {
            when(bankingRoleRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.assignPermission(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when permission does not exist")
        void shouldThrowWhenPermissionNotFound() {
            when(bankingRoleRepository.findById(10L)).thenReturn(Optional.of(tellerRole));
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
            when(bankingRoleRepository.findById(10L)).thenReturn(Optional.of(tellerRole));
            when(permissionRepository.existsById(1L)).thenReturn(true);
            when(bankingRoleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.removePermission(10L, 1L);

            assertThat(tellerRole.getPermissions()).isEmpty();
            verify(bankingRoleRepository).save(tellerRole);
        }

        @Test
        @DisplayName("Is idempotent — does not save when permission not assigned")
        void shouldBeIdempotentWhenNotAssigned() {
            tellerRole.setPermissions(new HashSet<>());  // empty
            when(bankingRoleRepository.findById(10L)).thenReturn(Optional.of(tellerRole));
            when(permissionRepository.existsById(1L)).thenReturn(true);

            service.removePermission(10L, 1L);

            verify(bankingRoleRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when role not found")
        void shouldThrowWhenRoleNotFound() {
            when(bankingRoleRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.removePermission(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when permission not found")
        void shouldThrowWhenPermissionNotFound() {
            when(bankingRoleRepository.findById(10L)).thenReturn(Optional.of(tellerRole));
            when(permissionRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> service.removePermission(10L, 99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
