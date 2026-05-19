package com.org.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.auth.dto.AdLoginRequestDto;
import com.org.auth.dto.LoginResponseDto;
import com.org.auth.dto.UserDto;
import com.org.auth.exception.AdAuthenticationException;
import com.org.auth.exception.handler.GlobalExceptionHandler;
import com.org.auth.service.AdAuthService;
import com.org.auth.utils.AuthProvider;
import com.org.auth.utils.UserStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link AdAuthController}.
 *
 * <p>Standalone MockMvc — Spring Security filter chain is NOT loaded. These tests
 * focus on request/response contract, input validation, and error propagation.
 * The {@link AdAuthService} is fully mocked; token crypto is not exercised here.
 */
@DisplayName("AdAuthController")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class AdAuthControllerTest {

    @Mock
    private AdAuthService adAuthService;

    private MockMvc      mockMvc;
    private ObjectMapper objectMapper;

    private static final String URL = "/auth/ad/login";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdAuthController(adAuthService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── Test fixture ──────────────────────────────────────────────────────────

    private LoginResponseDto buildLoginResponse() {
        UserDto user = new UserDto();
        user.setId(1L);
        user.setName("Alice Smith");
        user.setEmail("alice@corp.example.com");
        user.setStatus(UserStatus.ACTIVE);
        user.setAuthProvider(AuthProvider.AZURE_AD);
        user.setGroups(List.of("RETAIL_CUSTOMER"));
        user.setRoles(List.of());
        user.setEffectivePermissions(List.of());

        LoginResponseDto response = new LoginResponseDto();
        response.setAccessToken("eyAccess.token.here");
        response.setRefreshToken("eyRefresh.token.here");
        response.setUser(user);
        return response;
    }

    private String body(String idToken) throws Exception {
        AdLoginRequestDto req = new AdLoginRequestDto();
        req.setIdToken(idToken);
        return objectMapper.writeValueAsString(req);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POST /auth/ad/login
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /auth/ad/login")
    class AdLogin {

        @Test
        @DisplayName("Returns 200 SUCCESS with access token, refresh token and user profile on valid ID token")
        void shouldReturn200OnValidIdToken() throws Exception {
            when(adAuthService.adLogin(any())).thenReturn(buildLoginResponse());

            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("eyValidIdToken.signed.byAzure")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").value("AD login successful"))
                    .andExpect(jsonPath("$.data.accessToken").value("eyAccess.token.here"))
                    .andExpect(jsonPath("$.data.refreshToken").value("eyRefresh.token.here"))
                    .andExpect(jsonPath("$.data.user.email").value("alice@corp.example.com"))
                    .andExpect(jsonPath("$.data.user.authProvider").value("AZURE_AD"))
                    .andExpect(jsonPath("$.data.user.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("Returns 400 FAIL when idToken is blank (Jakarta @NotBlank constraint)")
        void shouldReturn400WhenIdTokenIsBlank() throws Exception {
            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("   ")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("FAIL"));
        }

        @Test
        @DisplayName("Returns 400 FAIL when idToken field is null in request body")
        void shouldReturn400WhenIdTokenIsNull() throws Exception {
            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idToken\":null}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("FAIL"));
        }

        @Test
        @DisplayName("Returns 400 FAIL on malformed JSON body")
        void shouldReturn400OnMalformedJson() throws Exception {
            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{not valid json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("FAIL"));
        }

        @Test
        @DisplayName("Returns 400 FAIL when request body is missing entirely")
        void shouldReturn400WhenBodyAbsent() throws Exception {
            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Returns 401 FAIL when service rejects an invalid or expired ID token")
        void shouldReturn401WhenTokenInvalid() throws Exception {
            when(adAuthService.adLogin(any()))
                    .thenThrow(new AdAuthenticationException("invalid or expired ID token"));

            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("eyExpired.or.tampered")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value("FAIL"))
                    .andExpect(jsonPath("$.message",
                            is("AD authentication failed: invalid or expired ID token")));
        }

        @Test
        @DisplayName("Returns 401 FAIL when AD login is disabled on the server")
        void shouldReturn401WhenAdDisabled() throws Exception {
            when(adAuthService.adLogin(any()))
                    .thenThrow(new AdAuthenticationException("AD login is not enabled on this server"));

            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("eyAnyToken")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value("FAIL"));
        }

        @Test
        @DisplayName("Returns 401 FAIL when user account is suspended")
        void shouldReturn401WhenAccountSuspended() throws Exception {
            when(adAuthService.adLogin(any()))
                    .thenThrow(new AdAuthenticationException("account is suspended"));

            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("eyValidToken")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value("FAIL"));
        }

        @Test
        @DisplayName("Returns 401 FAIL when issuer claim does not match trusted issuer")
        void shouldReturn401WhenIssuerMismatch() throws Exception {
            when(adAuthService.adLogin(any()))
                    .thenThrow(new AdAuthenticationException("token issuer is not trusted"));

            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("eyWrongIssuer")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message",
                            is("AD authentication failed: token issuer is not trusted")));
        }

        @Test
        @DisplayName("Returns 500 FAIL when an unexpected runtime exception occurs")
        void shouldReturn500OnUnexpectedException() throws Exception {
            when(adAuthService.adLogin(any()))
                    .thenThrow(new RuntimeException("DB unavailable"));

            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("eyValidToken")))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value("FAIL"));
        }
    }
}
