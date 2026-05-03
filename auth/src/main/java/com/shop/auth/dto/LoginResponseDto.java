package com.shop.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Login response — JWT tokens plus the authenticated user's profile")
public class LoginResponseDto {

    @Schema(description = "Short-lived access token (15 min) for authorising API calls")
    private String accessToken;

    @Schema(description = "Long-lived refresh token (7 days) for obtaining a new access token")
    private String refreshToken;

    @Schema(description = "Authenticated user's profile")
    private UserDto user;
}
