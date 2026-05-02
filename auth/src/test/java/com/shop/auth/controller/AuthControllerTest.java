package com.shop.auth.controller;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.auth.dto.RegisterRequestDto;
import com.shop.auth.exception.EmailAlreadyExistsException;
import com.shop.auth.exception.handler.GlobalExceptionHandler;
import com.shop.auth.fixtures.AddressDtoFixture;
import com.shop.auth.fixtures.RegisterRequestDtoFixture;
import com.shop.auth.service.AuthService;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AuthController")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class AuthControllerTest {

    @Mock private AuthService authService;

    private MockMvc      mockMvc;
    private ObjectMapper objectMapper;

    private static final String REGISTER_URL = "/auth/register";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders
            .standaloneSetup(new AuthController(authService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    // ── Happy path ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/register — success")
    class RegisterSuccess {

        @Test
        @DisplayName("Should return 201 CREATED with SUCCESS status for valid request")
        void shouldReturn201ForValidRequest() throws Exception {
            doNothing().when(authService).register(any());

            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(RegisterRequestDtoFixture.valid())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.errors").doesNotExist());
        }

        @Test
        @DisplayName("Should accept request without optional phone field")
        void shouldAcceptRequestWithoutPhone() throws Exception {
            RegisterRequestDto request = RegisterRequestDtoFixture.valid();
            request.setPhone(null);
            doNothing().when(authService).register(any());

            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Should accept request without optional role — service defaults to USER")
        void shouldAcceptRequestWithoutRole() throws Exception {
            doNothing().when(authService).register(any());

            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(RegisterRequestDtoFixture.withNullRole())))
                .andExpect(status().isCreated());
        }
    }

    // ── Validation failures ──────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/register — validation failures → 400")
    class ValidationFailures {

        @Test
        @DisplayName("Should return 400 when name is missing")
        void shouldReturn400WhenNameIsMissing() throws Exception {
            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(RegisterRequestDtoFixture.withNoName())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAIL"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[*]", hasItem(containsString("name"))));

            verify(authService, never()).register(any());
        }

        @Test
        @DisplayName("Should return 400 when email is missing")
        void shouldReturn400WhenEmailIsMissing() throws Exception {
            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(RegisterRequestDtoFixture.withNoEmail())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAIL"))
                .andExpect(jsonPath("$.errors[*]", hasItem(containsString("email"))));

            verify(authService, never()).register(any());
        }

        @Test
        @DisplayName("Should return 400 when email format is invalid")
        void shouldReturn400WhenEmailFormatIsInvalid() throws Exception {
            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(RegisterRequestDtoFixture.withEmail("not-an-email"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*]", hasItem(containsString("email"))));
        }

        @Test
        @DisplayName("Should return 400 when password is shorter than 8 characters")
        void shouldReturn400WhenPasswordIsTooShort() throws Exception {
            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(RegisterRequestDtoFixture.withPassword("short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*]", hasItem(containsString("password"))));
        }

        @Test
        @DisplayName("Should return 400 when addresses list is empty")
        void shouldReturn400WhenAddressesIsEmpty() throws Exception {
            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        RegisterRequestDtoFixture.withAddresses(Collections.emptyList()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
        }

        @Test
        @DisplayName("Should return 400 when nested address is missing required country — @Valid cascade")
        void shouldReturn400WhenNestedAddressIsInvalid() throws Exception {
            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        RegisterRequestDtoFixture.withAddresses(
                            List.of(AddressDtoFixture.withoutCountry())))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*]", hasItem(containsString("country"))));
        }

        @Test
        @DisplayName("Should collect all field errors in one response — no fail-fast")
        void shouldReturnMultipleErrorsAtOnce() throws Exception {
            RegisterRequestDto request = RegisterRequestDtoFixture.withNoName();
            request.setEmail(null);

            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors.length()", greaterThan(1)));
        }
    }

    // ── Business rule failures ───────────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/register — business rule failures")
    class BusinessFailures {

        @Test
        @DisplayName("Should return 409 CONFLICT when email is already registered")
        void shouldReturn409WhenEmailAlreadyExists() throws Exception {
            doThrow(new EmailAlreadyExistsException("john.doe@example.com"))
                .when(authService).register(any());

            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(RegisterRequestDtoFixture.valid())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("FAIL"))
                .andExpect(jsonPath("$.message", containsString("john.doe@example.com")));
        }
    }

    // ── Malformed request ────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/register — malformed request")
    class MalformedRequest {

        @Test
        @DisplayName("Should return 400 when body is not valid JSON")
        void shouldReturn400ForMalformedJson() throws Exception {
            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{invalid json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAIL"))
                .andExpect(jsonPath("$.message").value("Malformed JSON request"));

            verify(authService, never()).register(any());
        }

        @Test
        @DisplayName("Should return 400 when body is completely empty")
        void shouldReturn400ForEmptyBody() throws Exception {
            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(""))
                .andExpect(status().isBadRequest());
        }
    }
}
