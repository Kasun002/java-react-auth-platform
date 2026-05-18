package com.org.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request body to assign a permission to a role")
public class AssignPermissionToRoleRequestDto {

    @NotNull(message = "permissionId is required")
    @Schema(description = "ID of the permission to assign", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long permissionId;
}
