package com.shop.auth.service;

import com.shop.auth.dto.AdLoginRequestDto;
import com.shop.auth.dto.LoginResponseDto;

/**
 * Validates an Azure AD OIDC ID token, provisions / syncs the user record and
 * group memberships, and issues this service's own JWT pair.
 */
public interface AdAuthService {

    /**
     * Exchange an Azure AD ID token for a service-issued access + refresh token pair.
     *
     * @param request the ID token supplied by the client
     * @return login response containing the JWT pair and the user profile
     * @throws com.shop.auth.exception.AdAuthenticationException if the token is invalid
     *         or the AD login feature is disabled
     */
    LoginResponseDto adLogin(AdLoginRequestDto request);
}
