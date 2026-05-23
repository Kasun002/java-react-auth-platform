package com.org.auth.controller;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.auth.dto.LoginRequestDto;
import com.org.auth.dto.LoginResponseDto;
import com.org.auth.dto.RegisterRequestDto;
import com.org.auth.dto.UserDto;
import com.org.auth.dto.ResendOtpRequestDto;
import com.org.auth.dto.VerifyOtpRequestDto;
import com.org.auth.exception.AccountLockedException;
import com.org.auth.exception.EmailAlreadyExistsException;
import com.org.auth.exception.InvalidCredentialsException;
import com.org.auth.exception.OtpExpiredException;
import com.org.auth.exception.OtpInvalidException;
import com.org.auth.exception.OtpMaxAttemptsException;
import com.org.auth.exception.OtpResendLimitException;
import com.org.auth.exception.TooManyLoginAttemptsException;
import com.org.auth.exception.UserNotActiveException;
import com.org.auth.exception.handler.GlobalExceptionHandler;
import com.org.auth.fixtures.AddressDtoFixture;
import com.org.auth.fixtures.LoginRequestDtoFixture;
import com.org.auth.fixtures.RegisterRequestDtoFixture;
import com.org.auth.service.AuthService;
import com.org.auth.service.LoginRateLimitService;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AuthController")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class AuthControllerTest {

    @Mock private AuthService           authService;
    @Mock private LoginRateLimitService loginRateLimitService;

    private MockMvc      mockMvc;
    private ObjectMapper objectMapper;

    private static final String REGISTER_URL = "/auth/register";
    private static final String LOGIN_URL    = "/auth/login";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        // Allow login by default; individual tests can override.
        // lenient() is required because not every nested class exercises /auth/login.
        lenient().when(loginRateLimitService.checkAndIncrement(any())).thenReturn(true);
        mockMvc = MockMvcBuilders
            .standaloneSetup(new AuthController(authService, loginRateLimitService))
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
                .andExpect(jsonPath("$.message").value("Registration successful. An OTP has been sent to your email."))
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

    // ── Login — success ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/login — success")
    class LoginSuccess {

        @Test
        @DisplayName("Should return 200 OK with tokens and user profile on valid credentials")
        void shouldReturn200WithTokens() throws Exception {
            UserDto userDto = new UserDto();
            userDto.setName("John Doe");
            userDto.setEmail("john.doe@example.com");

            LoginResponseDto loginResponse = new LoginResponseDto();
            loginResponse.setAccessToken("access.token.value");
            loginResponse.setRefreshToken("refresh.token.value");
            loginResponse.setUser(userDto);

            when(authService.login(any(LoginRequestDto.class))).thenReturn(loginResponse);

            mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(LoginRequestDtoFixture.valid())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").value("access.token.value"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh.token.value"))
                .andExpect(jsonPath("$.data.user.name").value("John Doe"))
                .andExpect(jsonPath("$.data.user.email").value("john.doe@example.com"));
        }
    }

    // ── Login — validation failures ───────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/login — validation failures → 400")
    class LoginValidationFailures {

        @Test
        @DisplayName("Should return 400 when username is missing")
        void shouldReturn400WhenUsernameIsMissing() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(LoginRequestDtoFixture.withNoUsername())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAIL"))
                .andExpect(jsonPath("$.errors[*]", hasItem(containsString("username"))));

            verify(authService, never()).login(any());
        }

        @Test
        @DisplayName("Should return 400 when password is missing")
        void shouldReturn400WhenPasswordIsMissing() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(LoginRequestDtoFixture.withNoPassword())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*]", hasItem(containsString("password"))));

            verify(authService, never()).login(any());
        }
    }

    // ── Login — business failures ─────────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/login — business failures")
    class LoginBusinessFailures {

        @Test
        @DisplayName("Should return 401 UNAUTHORIZED for invalid credentials")
        void shouldReturn401ForInvalidCredentials() throws Exception {
            when(authService.login(any())).thenThrow(new InvalidCredentialsException());

            mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(LoginRequestDtoFixture.valid())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("FAIL"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }

        @Test
        @DisplayName("Should return 403 FORBIDDEN when account is not active")
        void shouldReturn403WhenAccountNotActive() throws Exception {
            when(authService.login(any())).thenThrow(new UserNotActiveException(UserStatus.NEW));

            mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(LoginRequestDtoFixture.valid())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("FAIL"))
                .andExpect(jsonPath("$.message", containsString("NEW")));
        }

        @Test
        @DisplayName("Should return 401 UNAUTHORIZED when account is locked — message includes lock expiry")
        void shouldReturn401WhenAccountIsLocked() throws Exception {
            when(authService.login(any())).thenThrow(
                    new AccountLockedException(java.time.LocalDateTime.now().plusMinutes(30)));

            mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(LoginRequestDtoFixture.valid())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("FAIL"))
                .andExpect(jsonPath("$.message", containsString("locked")));
        }

        @Test
        @DisplayName("Should return 429 TOO_MANY_REQUESTS when IP login rate limit is exceeded")
        void shouldReturn429WhenLoginRateLimitExceeded() throws Exception {
            when(loginRateLimitService.checkAndIncrement(any())).thenReturn(false);

            mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(LoginRequestDtoFixture.valid())))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value("FAIL"))
                .andExpect(jsonPath("$.message", containsString("Too many login attempts")));

            verify(authService, never()).login(any());
        }
    }

    // ── POST /auth/verify-otp ─────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/verify-otp")
    class VerifyOtp {

        private static final String VERIFY_URL = "/auth/verify-otp";

        private VerifyOtpRequestDto validRequest() {
            VerifyOtpRequestDto dto = new VerifyOtpRequestDto();
            dto.setEmail("john.doe@example.com");
            dto.setOtp("482910");
            return dto;
        }

        @Test
        @DisplayName("Should return 200 OK with success message on valid OTP")
        void shouldReturn200OnValidOtp() throws Exception {
            doNothing().when(authService).verifyOtp(any());

            mockMvc.perform(post(VERIFY_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Account verified successfully. You can now log in."));
        }

        @Test
        @DisplayName("Should return 400 when OTP format is invalid — not 6 digits")
        void shouldReturn400ForMalformedOtp() throws Exception {
            VerifyOtpRequestDto dto = validRequest();
            dto.setOtp("12AB");

            mockMvc.perform(post(VERIFY_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*]", hasItem(containsString("otp"))));

            verify(authService, never()).verifyOtp(any());
        }

        @Test
        @DisplayName("Should return 400 for invalid OTP value")
        void shouldReturn400OnInvalidOtp() throws Exception {
            doThrow(new OtpInvalidException()).when(authService).verifyOtp(any());

            mockMvc.perform(post(VERIFY_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAIL"));
        }

        @Test
        @DisplayName("Should return 400 for expired OTP")
        void shouldReturn400OnExpiredOtp() throws Exception {
            doThrow(new OtpExpiredException()).when(authService).verifyOtp(any());

            mockMvc.perform(post(VERIFY_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAIL"));
        }

        @Test
        @DisplayName("Should return 429 when max attempts are exceeded")
        void shouldReturn429OnMaxAttempts() throws Exception {
            doThrow(new OtpMaxAttemptsException()).when(authService).verifyOtp(any());

            mockMvc.perform(post(VERIFY_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value("FAIL"));
        }
    }

    // ── POST /auth/resend-otp ─────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/resend-otp")
    class ResendOtp {

        private static final String RESEND_URL = "/auth/resend-otp";

        private ResendOtpRequestDto validRequest() {
            ResendOtpRequestDto dto = new ResendOtpRequestDto();
            dto.setEmail("john.doe@example.com");
            return dto;
        }

        @Test
        @DisplayName("Should return 200 OK on successful resend")
        void shouldReturn200OnResend() throws Exception {
            doNothing().when(authService).resendOtp(any());

            mockMvc.perform(post(RESEND_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("OTP resent successfully. Please check your email."));
        }

        @Test
        @DisplayName("Should return 400 when email format is invalid")
        void shouldReturn400ForInvalidEmail() throws Exception {
            ResendOtpRequestDto dto = validRequest();
            dto.setEmail("not-an-email");

            mockMvc.perform(post(RESEND_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*]", hasItem(containsString("email"))));

            verify(authService, never()).resendOtp(any());
        }

        @Test
        @DisplayName("Should return 429 when resend rate limit is hit")
        void shouldReturn429OnResendLimit() throws Exception {
            doThrow(new OtpResendLimitException()).when(authService).resendOtp(any());

            mockMvc.perform(post(RESEND_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value("FAIL"));
        }
    }
}
