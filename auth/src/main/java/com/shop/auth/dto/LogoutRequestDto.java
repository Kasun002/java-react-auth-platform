package com.shop.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogoutRequestDto {

    @Schema(
        description = "The refresh token issued at login. Must be provided to fully revoke the session.",
        example = "eyJhbGciOiJIUzI1NiJ9..."
    )
    private String refreshToken;
}
