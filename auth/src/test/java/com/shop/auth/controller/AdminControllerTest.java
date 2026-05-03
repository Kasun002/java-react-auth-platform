package com.shop.auth.controller;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.auth.dto.AssignGroupRequestDto;
import com.shop.auth.dto.AssignPermissionToRoleRequestDto;
import com.shop.auth.dto.AssignRoleToGroupRequestDto;
import com.shop.auth.dto.BankingRoleDto;
import com.shop.auth.dto.PermissionDto;
import com.shop.auth.dto.UserGroupDto;
import com.shop.auth.exception.ResourceNotFoundException;
import com.shop.auth.exception.handler.GlobalExceptionHandler;
import com.shop.auth.service.BankingRoleService;
import com.shop.auth.service.PermissionService;
import com.shop.auth.service.UserGroupService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AdminController")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class AdminControllerTest {

    @Mock private PermissionService  permissionService;
    @Mock private BankingRoleService bankingRoleService;
    @Mock private UserGroupService   userGroupService;

    private MockMvc      mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminController(permissionService, bankingRoleService, userGroupService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PermissionDto makePermission(Long id, String code, String category) {
        PermissionDto dto = new PermissionDto();
        dto.setId(id);
        dto.setCode(code);
        dto.setCategory(category);
        return dto;
    }

    private BankingRoleDto makeRole(Long id, String name) {
        BankingRoleDto dto = new BankingRoleDto();
        dto.setId(id);
        dto.setName(name);
        dto.setPermissions(List.of(makePermission(1L, "ACCOUNT_VIEW", "ACCOUNT")));
        return dto;
    }

    private UserGroupDto makeGroup(Long id, String name, String type) {
        UserGroupDto dto = new UserGroupDto();
        dto.setId(id);
        dto.setName(name);
        dto.setType(type);
        dto.setRoles(List.of(makeRole(5L, "ROLE_CUSTOMER_BASIC")));
        return dto;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /admin/permissions
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /admin/permissions")
    class ListPermissions {

        @Test
        @DisplayName("Returns 200 with permission list")
        void shouldReturn200WithPermissions() throws Exception {
            when(permissionService.listAll()).thenReturn(List.of(
                    makePermission(1L, "ACCOUNT_VIEW", "ACCOUNT"),
                    makePermission(2L, "TRANSACTION_VIEW", "TRANSACTION")));

            mockMvc.perform(get("/admin/permissions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].code").value("ACCOUNT_VIEW"));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /admin/roles
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /admin/roles")
    class ListRoles {

        @Test
        @DisplayName("Returns 200 with role list including permissions")
        void shouldReturn200WithRoles() throws Exception {
            when(bankingRoleService.listAll()).thenReturn(List.of(makeRole(10L, "ROLE_TELLER")));

            mockMvc.perform(get("/admin/roles"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].name").value("ROLE_TELLER"))
                    .andExpect(jsonPath("$.data[0].permissions", hasSize(1)));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /admin/roles/{id}
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /admin/roles/{id}")
    class GetRole {

        @Test
        @DisplayName("Returns 200 with role detail")
        void shouldReturn200WhenFound() throws Exception {
            when(bankingRoleService.getById(10L)).thenReturn(makeRole(10L, "ROLE_TELLER"));

            mockMvc.perform(get("/admin/roles/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id", is(10)))
                    .andExpect(jsonPath("$.data.name").value("ROLE_TELLER"));
        }

        @Test
        @DisplayName("Returns 404 when role not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(bankingRoleService.getById(99L))
                    .thenThrow(new ResourceNotFoundException("Role", 99L));

            mockMvc.perform(get("/admin/roles/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value("FAIL"))
                    .andExpect(jsonPath("$.message", containsString("99")));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POST /admin/roles/{id}/permissions
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /admin/roles/{id}/permissions")
    class AssignPermissionToRole {

        @Test
        @DisplayName("Returns 200 with updated role on success")
        void shouldReturn200OnSuccess() throws Exception {
            AssignPermissionToRoleRequestDto req = new AssignPermissionToRoleRequestDto();
            req.setPermissionId(1L);

            when(bankingRoleService.assignPermission(10L, 1L)).thenReturn(makeRole(10L, "ROLE_TELLER"));

            mockMvc.perform(post("/admin/roles/10/permissions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.name").value("ROLE_TELLER"));
        }

        @Test
        @DisplayName("Returns 400 when permissionId is missing")
        void shouldReturn400WhenBodyInvalid() throws Exception {
            mockMvc.perform(post("/admin/roles/10/permissions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("FAIL"));
        }

        @Test
        @DisplayName("Returns 404 when role or permission not found")
        void shouldReturn404WhenNotFound() throws Exception {
            AssignPermissionToRoleRequestDto req = new AssignPermissionToRoleRequestDto();
            req.setPermissionId(99L);

            when(bankingRoleService.assignPermission(10L, 99L))
                    .thenThrow(new ResourceNotFoundException("Permission", 99L));

            mockMvc.perform(post("/admin/roles/10/permissions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DELETE /admin/roles/{id}/permissions/{permissionId}
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /admin/roles/{id}/permissions/{permissionId}")
    class RemovePermissionFromRole {

        @Test
        @DisplayName("Returns 204 on success")
        void shouldReturn204() throws Exception {
            doNothing().when(bankingRoleService).removePermission(10L, 1L);

            mockMvc.perform(delete("/admin/roles/10/permissions/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Returns 404 when role not found")
        void shouldReturn404WhenRoleNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("Role", 99L))
                    .when(bankingRoleService).removePermission(eq(99L), anyLong());

            mockMvc.perform(delete("/admin/roles/99/permissions/1"))
                    .andExpect(status().isNotFound());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /admin/groups
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /admin/groups")
    class ListGroups {

        @Test
        @DisplayName("Returns 200 with group list")
        void shouldReturn200WithGroups() throws Exception {
            when(userGroupService.listAll()).thenReturn(List.of(
                    makeGroup(1L, "RETAIL_CUSTOMER", "CUSTOMER")));

            mockMvc.perform(get("/admin/groups"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].name").value("RETAIL_CUSTOMER"))
                    .andExpect(jsonPath("$.data[0].type").value("CUSTOMER"));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POST /admin/groups/{id}/roles
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /admin/groups/{id}/roles")
    class AssignRoleToGroup {

        @Test
        @DisplayName("Returns 200 with updated group")
        void shouldReturn200() throws Exception {
            AssignRoleToGroupRequestDto req = new AssignRoleToGroupRequestDto();
            req.setRoleId(5L);

            when(userGroupService.assignRoleToGroup(1L, 5L))
                    .thenReturn(makeGroup(1L, "RETAIL_CUSTOMER", "CUSTOMER"));

            mockMvc.perform(post("/admin/groups/1/roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.name").value("RETAIL_CUSTOMER"));
        }

        @Test
        @DisplayName("Returns 400 when roleId is missing")
        void shouldReturn400WhenInvalid() throws Exception {
            mockMvc.perform(post("/admin/groups/1/roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DELETE /admin/groups/{id}/roles/{roleId}
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /admin/groups/{id}/roles/{roleId}")
    class RemoveRoleFromGroup {

        @Test
        @DisplayName("Returns 204 on success")
        void shouldReturn204() throws Exception {
            doNothing().when(userGroupService).removeRoleFromGroup(1L, 5L);

            mockMvc.perform(delete("/admin/groups/1/roles/5"))
                    .andExpect(status().isNoContent());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /admin/users/{userId}/groups
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /admin/users/{userId}/groups")
    class GetUserGroups {

        @Test
        @DisplayName("Returns 200 with user's groups")
        void shouldReturn200() throws Exception {
            when(userGroupService.getUserGroups(42L))
                    .thenReturn(List.of(makeGroup(1L, "RETAIL_CUSTOMER", "CUSTOMER")));

            mockMvc.perform(get("/admin/users/42/groups"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].name").value("RETAIL_CUSTOMER"));
        }

        @Test
        @DisplayName("Returns 404 when user not found")
        void shouldReturn404WhenUserNotFound() throws Exception {
            when(userGroupService.getUserGroups(99L))
                    .thenThrow(new ResourceNotFoundException("User", 99L));

            mockMvc.perform(get("/admin/users/99/groups"))
                    .andExpect(status().isNotFound());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POST /admin/users/{userId}/groups
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /admin/users/{userId}/groups")
    class AddUserToGroup {

        @Test
        @DisplayName("Returns 200 on success")
        void shouldReturn200() throws Exception {
            AssignGroupRequestDto req = new AssignGroupRequestDto();
            req.setGroupId(1L);
            doNothing().when(userGroupService).addUserToGroup(42L, 1L);

            mockMvc.perform(post("/admin/users/42/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"));
        }

        @Test
        @DisplayName("Returns 400 when groupId is missing")
        void shouldReturn400WhenInvalid() throws Exception {
            mockMvc.perform(post("/admin/users/42/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DELETE /admin/users/{userId}/groups/{groupId}
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /admin/users/{userId}/groups/{groupId}")
    class RemoveUserFromGroup {

        @Test
        @DisplayName("Returns 204 on success")
        void shouldReturn204() throws Exception {
            doNothing().when(userGroupService).removeUserFromGroup(42L, 1L);

            mockMvc.perform(delete("/admin/users/42/groups/1"))
                    .andExpect(status().isNoContent());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /admin/users/{userId}/permissions
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /admin/users/{userId}/permissions")
    class GetUserEffectivePermissions {

        @Test
        @DisplayName("Returns 200 with effective permission set")
        void shouldReturn200WithPermissions() throws Exception {
            when(userGroupService.getEffectivePermissions(42L))
                    .thenReturn(Set.of("ACCOUNT_VIEW", "TRANSACTION_VIEW"));

            mockMvc.perform(get("/admin/users/42/permissions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("Returns 404 when user not found")
        void shouldReturn404WhenUserNotFound() throws Exception {
            when(userGroupService.getEffectivePermissions(99L))
                    .thenThrow(new ResourceNotFoundException("User", 99L));

            mockMvc.perform(get("/admin/users/99/permissions"))
                    .andExpect(status().isNotFound());
        }
    }
}
