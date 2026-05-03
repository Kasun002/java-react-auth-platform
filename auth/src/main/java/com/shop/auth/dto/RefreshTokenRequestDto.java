package com.shop.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshTokenRequestDto {

    @NotBlank(message = "Refresh token is required")
    @Schema(
        description = "The refresh token issued at login or from a previous refresh call.",
        example = "eyJhbGciOiJIUzI1NiJ9..."
    )
    private String refreshToken;
}
