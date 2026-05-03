package com.shop.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "New token pair issued after a successful refresh. The old refresh token is revoked.")
public class RefreshTokenResponseDto {

    @Schema(description = "New short-lived access token for authorising API calls")
    private final String accessToken;

    @Schema(description = "New long-lived refresh token. Use this on the next refresh — the previous one is now revoked.")
    private final String refreshToken;
}
