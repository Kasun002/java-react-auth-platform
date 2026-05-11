package com.shop.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.auth.dto.AdLoginRequestDto;
import com.shop.auth.dto.LoginResponseDto;
import com.shop.auth.dto.ResponseDto;
import com.shop.auth.service.AdAuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles Azure AD / OIDC-based authentication.
 *
 * <p>
 * The client (SPA or mobile app) authenticates with Azure AD using MSAL / PKCE,
 * receives an OIDC ID token, and exchanges it here for this service's own JWT
 * pair.
 * The AD ID token is never forwarded to downstream services — only this
 * service's
 * short-lived access tokens are used for API calls.
 */
@Slf4j
@Tag(name = "AD Authentication", description = "Azure Active Directory / OIDC login")
@RestController
@RequestMapping("/auth/ad")
@RequiredArgsConstructor
public class AdAuthController {

    private final AdAuthService adAuthService;

    @Operation(summary = "Exchange an Azure AD ID token for a service JWT pair", description = "Validates the OIDC ID token issued by Azure AD (or Keycloak in dev), "
            +
            "provisions the user on first login, syncs LDAP group memberships, " +
            "and returns an access + refresh token pair.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(schema = @Schema(implementation = LoginResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error — idToken is blank"),
            @ApiResponse(responseCode = "401", description = "Invalid, expired, or untrusted ID token"),
            @ApiResponse(responseCode = "503", description = "AD login feature disabled on this server")
    })
    @PostMapping("/login")
    public ResponseEntity<ResponseDto<LoginResponseDto>> adLogin(
            @Valid @RequestBody AdLoginRequestDto request) {

        log.debug("AD login request received");
        LoginResponseDto loginResponse = adAuthService.adLogin(request);

        ResponseDto<LoginResponseDto> response = new ResponseDto<>();
        response.setStatus(ResponseDto.Status.SUCCESS);
        response.setMessage("AD login successful");
        response.setData(loginResponse);
        return ResponseEntity.ok(response);
    }
}
