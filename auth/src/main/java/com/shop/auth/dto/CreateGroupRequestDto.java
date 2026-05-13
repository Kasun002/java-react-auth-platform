package com.shop.auth.dto;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request body to create a new user group")
public class CreateGroupRequestDto {

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

    @Schema(description = "IDs of roles to assign to this group. Leave empty for no roles.")
    private List<Long> roleIds = new ArrayList<>();
}
