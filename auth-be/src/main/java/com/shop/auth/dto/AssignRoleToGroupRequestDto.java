package com.shop.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request body to assign a role to a group")
public class AssignRoleToGroupRequestDto {

    @NotNull(message = "roleId is required")
    @Schema(description = "ID of the role to assign", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long roleId;
}
