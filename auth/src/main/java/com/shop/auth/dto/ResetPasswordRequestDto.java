package com.shop.auth.dto;

import com.shop.auth.validation.StrongPassword;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequestDto {

    @NotBlank(message = "Reset token is required")
    @Schema(description = "The single-use reset token received by email", example = "a3f9e2c1-...")
    private String token;

    @NotBlank(message = "New password is required")
    @StrongPassword
    @Schema(
        description = "New password — min 12 chars, must include uppercase, lowercase, digit, and special character",
        example = "NewSecure@Pass2!"
    )
    private String newPassword;
}
