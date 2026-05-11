package com.shop.auth.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Role with its assigned permissions")
public class RoleDto {

    @Schema(description = "Role ID", example = "1")
    private Long id;

    @Schema(description = "Role name", example = "ROLE_MANAGER")
    private String name;

    @Schema(description = "Role description", example = "Department manager with approval authority")
    private String description;

    @Schema(description = "Permissions assigned to this role")
    private List<PermissionDto> permissions;
}
