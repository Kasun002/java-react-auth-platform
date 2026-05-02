package com.shop.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.auth.dto.LoginRequestDto;
import com.shop.auth.dto.LoginResponseDto;
import com.shop.auth.dto.RegisterRequestDto;
import com.shop.auth.dto.ResendOtpRequestDto;
import com.shop.auth.dto.ResponseDto;
import com.shop.auth.dto.VerifyOtpRequestDto;
import com.shop.auth.service.AuthService;
import com.shop.auth.utils.MaskingUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Tag(name = "Authentication", description = "User registration and authentication endpoints")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
        summary = "Register a new user",
        description = "Creates a new user account. Defaults role to USER and status to NEW if not provided."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User registered successfully",
            content = @Content(schema = @Schema(implementation = ResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed — check the errors field",
            content = @Content(schema = @Schema(implementation = ResponseDto.class))),
        @ApiResponse(responseCode = "409", description = "Email already in use",
            content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<ResponseDto<Void>> register(@Valid @RequestBody RegisterRequestDto request) {
        log.info("Register request received for email=[{}] name=[{}]",
                MaskingUtil.maskEmail(request.getEmail()), request.getName());

        authService.register(request);

        ResponseDto<Void> response = new ResponseDto<>();
        response.setStatus(ResponseDto.Status.SUCCESS);
        response.setMessage("Registration successful. An OTP has been sent to your email.");

        log.info("Register completed successfully for email=[{}]", MaskingUtil.maskEmail(request.getEmail()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
        summary = "Login",
        description = "Authenticates user credentials and returns JWT access + refresh tokens."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful",
            content = @Content(schema = @Schema(implementation = ResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed — check the errors field",
            content = @Content(schema = @Schema(implementation = ResponseDto.class))),
        @ApiResponse(responseCode = "401", description = "Invalid email or password",
            content = @Content(schema = @Schema(implementation = ResponseDto.class))),
        @ApiResponse(responseCode = "403", description = "Account is not active",
            content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<ResponseDto<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
        log.info("Login request received for username/email=[{}]",
                MaskingUtil.maskEmail(request.getUsername()));

        LoginResponseDto loginResponse = authService.login(request);

        ResponseDto<LoginResponseDto> response = new ResponseDto<>();
        response.setStatus(ResponseDto.Status.SUCCESS);
        response.setMessage("Login successful");
        response.setData(loginResponse);

        log.info("Login completed successfully for username/email=[{}]",
                MaskingUtil.maskEmail(request.getUsername()));
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Verify OTP",
        description = "Validates the 6-digit OTP sent after registration. Activates the account on success."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Account verified and activated",
            content = @Content(schema = @Schema(implementation = ResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid OTP, expired OTP, or validation error",
            content = @Content(schema = @Schema(implementation = ResponseDto.class))),
        @ApiResponse(responseCode = "429", description = "Maximum OTP attempts exceeded",
            content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("/verify-otp")
    public ResponseEntity<ResponseDto<Void>> verifyOtp(@Valid @RequestBody VerifyOtpRequestDto request) {
        log.info("OTP verify request for email=[{}]", MaskingUtil.maskEmail(request.getEmail()));

        authService.verifyOtp(request);

        ResponseDto<Void> response = new ResponseDto<>();
        response.setStatus(ResponseDto.Status.SUCCESS);
        response.setMessage("Account verified successfully. You can now log in.");
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Resend OTP",
        description = "Issues a fresh 6-digit OTP to the registered email. Rate-limited to 3 requests per hour."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OTP resent successfully",
            content = @Content(schema = @Schema(implementation = ResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid email or account not in a verifiable state",
            content = @Content(schema = @Schema(implementation = ResponseDto.class))),
        @ApiResponse(responseCode = "429", description = "Resend rate limit exceeded",
            content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("/resend-otp")
    public ResponseEntity<ResponseDto<Void>> resendOtp(@Valid @RequestBody ResendOtpRequestDto request) {
        log.info("OTP resend request for email=[{}]", MaskingUtil.maskEmail(request.getEmail()));

        authService.resendOtp(request);

        ResponseDto<Void> response = new ResponseDto<>();
        response.setStatus(ResponseDto.Status.SUCCESS);
        response.setMessage("OTP resent successfully. Please check your email.");
        return ResponseEntity.ok(response);
    }
}
