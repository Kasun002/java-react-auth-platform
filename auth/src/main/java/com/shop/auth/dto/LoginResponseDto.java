package com.shop.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Standard Login API data wrapper")
public class LoginResponseDto {
    @Schema(description = "Access token for private api calls")
    private String accessToken;

    @Schema(description = "Refresh token for update short lived access token")
    private String refreshToken;

    @Schema(description = "User name")
    private String name;
}
