package com.shop.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for POST /auth/ad/login.
 *
 * <p>
 * The client (SPA or mobile app) obtains an OIDC ID token from Azure AD
 * (or Keycloak in dev) using MSAL / OAuth2 PKCE flow, then exchanges it here
 * for this service's own JWT pair.
 */
@Data
@Schema(description = "Exchange an Azure AD OIDC ID token for a service-issued JWT pair")
public class AdLoginRequestDto {

    @NotBlank
    @Schema(description = "OIDC ID token issued by Azure AD (or Keycloak in dev). " +
            "Obtained by the client via MSAL / OAuth2 PKCE flow.", example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String idToken;
}
