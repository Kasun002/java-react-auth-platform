package com.org.auth.exception.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.auth.dto.RegisterRequestDto;
import com.org.auth.exception.BusinessException;
import com.org.auth.exception.EmailAlreadyExistsException;
import com.org.auth.fixtures.RegisterRequestDtoFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Standalone test for GlobalExceptionHandler.
 * Uses a probe controller to trigger each exception type in isolation —
 * no Spring context required, fast execution.
 */
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private MockMvc      mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders
            .standaloneSetup(new ProbeController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    // ── Probe controller — triggers specific exceptions on demand ─────────────

    @RestController
    static class ProbeController {

        @GetMapping("/probe/email-conflict")
        void throwEmailConflict() {
            throw new EmailAlreadyExistsException("test@example.com");
        }

        @GetMapping("/probe/custom-business-400")
        void throwCustomBusiness400() {
            throw new BusinessException("Custom rule violated", HttpStatus.BAD_REQUEST);
        }

        @PostMapping("/probe/validation")
        void triggerValidation(@Valid @RequestBody RegisterRequestDto body) {}

        @GetMapping("/probe/unexpected")
        void throwUnexpected() {
            throw new RuntimeException("Something went terribly wrong");
        }
    }

    // ── BusinessException ────────────────────────────────────────────────────

    @Nested
    @DisplayName("BusinessException")
    class BusinessExceptions {

        @Test
        @DisplayName("Should return 409 CONFLICT for EmailAlreadyExistsException")
        void shouldReturn409ForEmailConflict() throws Exception {
            mockMvc.perform(get("/probe/email-conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("FAIL"))
                .andExpect(jsonPath("$.message", containsString("test@example.com")));
        }

        @Test
        @DisplayName("Should return the HTTP status embedded in the exception")
        void shouldReturnStatusFromException() throws Exception {
            mockMvc.perform(get("/probe/custom-business-400"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAIL"))
                .andExpect(jsonPath("$.message").value("Custom rule violated"));
        }

        @Test
        @DisplayName("Response body must not contain a data field for business errors")
        void shouldNotContainDataFieldForBusinessErrors() throws Exception {
            mockMvc.perform(get("/probe/email-conflict"))
                .andExpect(jsonPath("$.data").doesNotExist());
        }
    }

    // ── MethodArgumentNotValidException ──────────────────────────────────────

    @Nested
    @DisplayName("Validation failures")
    class ValidationFailures {

        @Test
        @DisplayName("Should return 400 with FAIL status and field-level errors array")
        void shouldReturn400WithFieldErrors() throws Exception {
            RegisterRequestDto invalid = RegisterRequestDtoFixture.withNoName();

            mockMvc.perform(post("/probe/validation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAIL"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[*]", hasItem(containsString("name"))));
        }

        @Test
        @DisplayName("Should accumulate errors from all invalid fields at once")
        void shouldAccumulateAllFieldErrors() throws Exception {
            RegisterRequestDto invalid = RegisterRequestDtoFixture.withNoName();
            invalid.setEmail(null);

            mockMvc.perform(post("/probe/validation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()", is(org.hamcrest.Matchers.greaterThan(1))));
        }
    }

    // ── HttpMessageNotReadableException ──────────────────────────────────────

    @Nested
    @DisplayName("Malformed JSON")
    class MalformedJson {

        @Test
        @DisplayName("Should return 400 with 'Malformed JSON request' message")
        void shouldReturn400ForMalformedJson() throws Exception {
            mockMvc.perform(post("/probe/validation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAIL"))
                .andExpect(jsonPath("$.message").value("Malformed JSON request"));
        }
    }

    // ── Generic fallback ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Unexpected exceptions")
    class UnexpectedExceptions {

        @Test
        @DisplayName("Should return 500 with generic message — no internal detail leaked")
        void shouldReturn500WithGenericMessage() throws Exception {
            mockMvc.perform(get("/probe/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("FAIL"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
        }

        @Test
        @DisplayName("Should never leak internal exception message to client")
        void shouldNeverLeakInternalDetails() throws Exception {
            mockMvc.perform(get("/probe/unexpected"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.not(
                    containsString("Something went terribly wrong"))));
        }
    }
}
