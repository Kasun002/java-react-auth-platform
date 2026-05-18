package com.org.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request body to add a user to a group")
public class AssignGroupRequestDto {

    @NotNull(message = "groupId is required")
    @Schema(description = "ID of the group to add the user to", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long groupId;
}
