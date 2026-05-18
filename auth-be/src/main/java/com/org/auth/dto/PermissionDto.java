package com.org.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Permission — atomic operation code")
public class PermissionDto {

    @Schema(description = "Permission ID", example = "1")
    private Long id;

    @Schema(description = "Permission code used in @PreAuthorize", example = "ACCOUNT_VIEW")
    private String code;

    @Schema(description = "Human-readable description", example = "View account details and balance")
    private String description;

    @Schema(description = "Domain category", example = "ACCOUNT")
    private String category;
}
