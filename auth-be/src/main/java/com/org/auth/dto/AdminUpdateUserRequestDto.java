package com.org.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Admin request to update a user's profile fields")
public class AdminUpdateUserRequestDto {

    @Schema(description = "Full name", example = "Jane Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Name is required")
    @Size(max = 255)
    private String name;

    @Schema(description = "Email address — uniqueness is enforced; existing sessions are NOT invalidated on change", example = "jane.doe@corp.example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    @Size(max = 255)
    private String email;

    @Schema(description = "Contact phone number", example = "+94771234567")
    @Size(max = 50)
    private String phone;
}
