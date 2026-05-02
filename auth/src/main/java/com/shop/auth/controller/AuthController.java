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
import com.shop.auth.dto.ResponseDto;
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
        response.setMessage("User registered successfully");

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
}
