package com.shop.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request body to update an existing user group")
public class UpdateGroupRequestDto {

    @NotBlank(message = "name is required")
    @Size(max = 100, message = "name must be at most 100 characters")
    @Schema(description = "Unique group name", example = "OPERATIONS_TEAM", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "type is required")
    @Size(max = 50, message = "type must be at most 50 characters")
    @Schema(description = "Group type — free-form, defined by the organization", example = "STAFF", requiredMode = Schema.RequiredMode.REQUIRED)
    private String type;

    @Size(max = 500, message = "description must be at most 500 characters")
    @Schema(description = "Human-readable description of the group", example = "Operations department users")
    private String description;
}
