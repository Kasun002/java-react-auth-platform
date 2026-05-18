package com.org.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request to change the local UserGroup that an AD group maps to")
public class UpdateAdGroupMappingRequestDto {

    @NotNull
    @Schema(description = "ID of the new local UserGroup", example = "5")
    private Long localGroupId;
}
