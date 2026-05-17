package com.shop.auth.dto;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request body to update an existing role")
public class UpdateRoleRequestDto {

    @NotBlank(message = "name is required")
    @Size(max = 100, message = "name must be at most 100 characters")
    @Schema(description = "Unique role name", example = "ROLE_MANAGER", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 500, message = "description must be at most 500 characters")
    @Schema(description = "Human-readable description of the role", example = "Can manage team members")
    private String description;

    @Schema(description = "Full replacement of the role's permission assignments. Send the complete desired set — any permissions not in this list are removed.")
    private List<Long> permissionIds = new ArrayList<>();
}
