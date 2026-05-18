package com.org.auth.dto;

import java.util.ArrayList;
import java.util.List;

import com.org.auth.validation.StrongPassword;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Admin request to provision a new user account")
public class AdminCreateUserRequestDto {

    @Schema(description = "Full name", example = "Jane Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Name is required")
    @Size(max = 255)
    private String name;

    @Schema(description = "Email address — must be unique", example = "jane.doe@corp.example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    @Size(max = 255)
    private String email;

    @Schema(description = "Temporary password — must satisfy PCI-DSS strength rules. User should change on first login.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Temporary password is required")
    @StrongPassword
    private String temporaryPassword;

    @Schema(description = "Contact phone number", example = "+94771234567")
    @Size(max = 50)
    private String phone;

    @Schema(description = "IDs of groups to add the user to immediately. Leave empty to assign no groups.")
    private List<Long> groupIds = new ArrayList<>();
}
