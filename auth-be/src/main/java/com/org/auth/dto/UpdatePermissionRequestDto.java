package com.org.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request body to update an existing permission")
public class UpdatePermissionRequestDto {

    @NotBlank(message = "code is required")
    @Size(max = 100, message = "code must be at most 100 characters")
    @Schema(description = "Unique permission code", example = "INVOICE_CREATE", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @NotBlank(message = "category is required")
    @Size(max = 50, message = "category must be at most 50 characters")
    @Schema(description = "Category that groups related permissions", example = "INVOICE", requiredMode = Schema.RequiredMode.REQUIRED)
    private String category;

    @Size(max = 500, message = "description must be at most 500 characters")
    @Schema(description = "Human-readable description", example = "Allows creating invoices")
    private String description;
}
