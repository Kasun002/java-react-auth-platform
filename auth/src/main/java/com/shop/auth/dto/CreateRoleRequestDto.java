package com.shop.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request body to create a new role")
public class CreateRoleRequestDto {

    @NotBlank(message = "name is required")
    @Size(max = 100, message = "name must be at most 100 characters")
    @Schema(description = "Unique role name (e.g. ROLE_MANAGER)", example = "ROLE_MANAGER", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 500, message = "description must be at most 500 characters")
    @Schema(description = "Human-readable description of the role", example = "Can manage team members")
    private String description;
}
