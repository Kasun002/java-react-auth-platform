package com.org.auth.dto;

import com.org.auth.utils.UserStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request to change a user's account status")
public class UpdateUserStatusRequestDto {

    @Schema(
        description = "Target status. SUSPENDED immediately invalidates all active tokens. "
                    + "NEW and DELETED are system-managed and cannot be set via this endpoint.",
        example = "SUSPENDED",
        allowableValues = {"ACTIVE", "INACTIVE", "SUSPENDED"},
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Status is required")
    private UserStatus status;
}
