package com.shop.auth.dto;

import java.util.ArrayList;
import java.util.List;

import com.shop.auth.utils.Role;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request payload for user registration")
public class RegisterRequestDto {

    @Schema(description = "Full name of the user", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be at most 255 characters")
    private String name;

    @Schema(description = "User email address", example = "john.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Email is required")
    @Email(message = "Email is not valid format")
    @Size(max = 255, message = "Email must be at most 255 characters")
    private String email;

    @Schema(description = "Contact phone number", example = "+94771234567")
    @Size(max = 50, message = "Phone number must be at most 50 characters")
    private String phone;

    @Schema(description = "Password — minimum 8 characters", example = "Secret@123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @Schema(description = "At least one address is required", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull(message = "Addresses list is required")
    @Size(min = 1, message = "At least one address is required")
    private List<AddressDto> addresses = new ArrayList<>();

    @Schema(description = "User role — defaults to USER if omitted", example = "USER")
    private Role role;
}