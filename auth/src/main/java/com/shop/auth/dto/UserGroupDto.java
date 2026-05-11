package com.shop.auth.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "User group with its assigned roles")
public class UserGroupDto {

    @Schema(description = "Group ID", example = "1")
    private Long id;

    @Schema(description = "Group name", example = "SYSTEM_ADMIN")
    private String name;

    @Schema(description = "Group description")
    private String description;

    @Schema(description = "Group type — free-form, defined by the organization", example = "ADMIN")
    private String type;

    @Schema(description = "Roles assigned to this group")
    private List<RoleDto> roles;
}
