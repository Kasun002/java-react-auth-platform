package com.shop.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.auth.dto.ChangePasswordRequestDto;
import com.shop.auth.dto.ForgotPasswordRequestDto;
import com.shop.auth.dto.LoginRequestDto;
import com.shop.auth.dto.LoginResponseDto;
import com.shop.auth.dto.LogoutRequestDto;
import com.shop.auth.dto.RefreshTokenRequestDto;
import com.shop.auth.dto.RefreshTokenResponseDto;
import com.shop.auth.dto.RegisterRequestDto;
import com.shop.auth.dto.ResendOtpRequestDto;
import com.shop.auth.dto.ResetPasswordRequestDto;
import com.shop.auth.dto.ResponseDto;
import com.shop.auth.dto.VerifyOtpRequestDto;
import com.shop.auth.service.AuthService;
import com.shop.auth.utils.MaskingUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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

        @Operation(summary = "Register a new user", description = "Creates a new user account. Defaults role to USER and status to NEW if not provided.")
        @ApiResponse(responseCode = "201", description = "User registered successfully", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
        @ApiResponse(responseCode = "400", description = "Validation failed — check the errors field", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
        @ApiResponse(responseCode = "409", description = "Email already in use", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
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

        @Operation(summary = "Login", description = "Authenticates user credentials and returns JWT access + refresh tokens.")
        @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
        @ApiResponse(responseCode = "400", description = "Validation failed — check the errors field", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
        @ApiResponse(responseCode = "401", description = "Invalid email or password", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
        @ApiResponse(responseCode = "403", description = "Account is not active", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
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

        @Operation(summary = "Verify OTP", description = "Validates the 6-digit OTP sent after registration. Activates the account on success.")
        @ApiResponse(responseCode = "200", description = "Account verified and activated", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
        @ApiResponse(responseCode = "400", description = "Invalid OTP, expired OTP, or validation error", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
        @ApiResponse(responseCode = "429", description = "Maximum OTP attempts exceeded", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
        @PostMapping("/verify-otp")
        public ResponseEntity<ResponseDto<Void>> verifyOtp(@Valid @RequestBody VerifyOtpRequestDto request) {
                log.info("OTP verify request for email=[{}]", MaskingUtil.maskEmail(request.getEmail()));

                authService.verifyOtp(request);

                ResponseDto<Void> response = new ResponseDto<>();
                response.setStatus(ResponseDto.Status.SUCCESS);
                response.setMessage("Account verified successfully. You can now log in.");
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Resend OTP", description = "Issues a fresh 6-digit OTP to the registered email. Rate-limited to 3 requests per hour.")
        @ApiResponse(responseCode = "200", description = "OTP resent successfully", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
        @ApiResponse(responseCode = "400", description = "Invalid email or account not in a verifiable state", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
        @ApiResponse(responseCode = "429", description = "Resend rate limit exceeded", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
        @PostMapping("/resend-otp")
        public ResponseEntity<ResponseDto<Void>> resendOtp(@Valid @RequestBody ResendOtpRequestDto request) {
                log.info("OTP resend request for email=[{}]", MaskingUtil.maskEmail(request.getEmail()));

                authService.resendOtp(request);

                ResponseDto<Void> response = new ResponseDto<>();
                response.setStatus(ResponseDto.Status.SUCCESS);
                response.setMessage("OTP resent successfully. Please check your email.");
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Change password", description = "Changes the authenticated user's password. Requires the current password for verification. "
                        + "All active sessions (all devices) are invalidated immediately — the user must log in again.")
        @ApiResponse(responseCode = "200", description = "Password changed successfully. All sessions revoked.", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
        @ApiResponse(responseCode = "400", description = "New password fails complexity or history rules", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
        @ApiResponse(responseCode = "401", description = "Current password is incorrect or token is invalid", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
        @PostMapping("/change-password")
        public ResponseEntity<ResponseDto<Void>> changePassword(
                        HttpServletRequest httpRequest,
                        @Valid @RequestBody ChangePasswordRequestDto body) {

                String accessToken = httpRequest.getHeader("Authorization").substring(7);
                log.info("Change-password request received");
                authService.changePassword(accessToken, body);

                ResponseDto<Void> response = new ResponseDto<>();
                response.setStatus(ResponseDto.Status.SUCCESS);
                response.setMessage("Password changed successfully. Please log in again on all devices.");
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Refresh tokens", description = "Exchanges a valid refresh token for a new access + refresh token pair (refresh token rotation). "
                        + "The supplied refresh token is revoked immediately after the new pair is issued — "
                        + "each refresh token can only be used once.")
        @ApiResponse(responseCode = "200", description = "New token pair issued", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
        @ApiResponse(responseCode = "400", description = "Validation failed — refresh token is required", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
        @ApiResponse(responseCode = "401", description = "Refresh token is invalid, expired, or already used", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
        @PostMapping("/refresh")
        public ResponseEntity<ResponseDto<RefreshTokenResponseDto>> refresh(
                        @Valid @RequestBody RefreshTokenRequestDto request) {

                log.info("Token refresh request received");
                RefreshTokenResponseDto tokens = authService.refresh(request);

                ResponseDto<RefreshTokenResponseDto> response = new ResponseDto<>();
                response.setStatus(ResponseDto.Status.SUCCESS);
                response.setMessage("Token refreshed successfully");
                response.setData(tokens);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Forgot password", description = "Sends a password reset link to the registered email address. "
                        + "Always returns 200 regardless of whether the email exists — prevents account enumeration.")
        @ApiResponse(responseCode = "200", description = "If the email is registered, a reset link has been sent", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
        @PostMapping("/forgot-password")
        public ResponseEntity<ResponseDto<Void>> forgotPassword(
                        @Valid @RequestBody ForgotPasswordRequestDto request) {

                log.info("Forgot-password request for email=[{}]",
                                MaskingUtil.maskEmail(request.getEmail()));
                authService.forgotPassword(request);

                ResponseDto<Void> response = new ResponseDto<>();
                response.setStatus(ResponseDto.Status.SUCCESS);
                response.setMessage("If that email is registered, a password reset link has been sent.");
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Reset password", description = "Resets the user's password using the single-use token from the reset email. "
                        + "All active sessions are invalidated on success.")
        @ApiResponse(responseCode = "200", description = "Password reset successfully. Please log in again.", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
        @ApiResponse(responseCode = "400", description = "Token is invalid/expired, or new password fails policy", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
        @PostMapping("/reset-password")
        public ResponseEntity<ResponseDto<Void>> resetPassword(
                        @Valid @RequestBody ResetPasswordRequestDto request) {

                log.info("Reset-password request received");
                authService.resetPassword(request);

                ResponseDto<Void> response = new ResponseDto<>();
                response.setStatus(ResponseDto.Status.SUCCESS);
                response.setMessage("Password reset successfully. Please log in with your new password.");
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Logout", description = "Revokes the current session by blacklisting both the access and refresh token JTIs "
                        + "in Redis. The access token is taken from the Authorization header. "
                        + "The refresh token should be provided in the request body to fully invalidate the session.")
        @ApiResponse(responseCode = "200", description = "Logged out successfully", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
        @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
        @PostMapping("/logout")
        public ResponseEntity<ResponseDto<Void>> logout(
                        HttpServletRequest httpRequest,
                        @RequestBody(required = false) LogoutRequestDto body) {

                // The Authorization header is guaranteed to be present and valid at this point
                // because JwtAuthenticationFilter has already validated it before this method
                // runs.
                String accessToken = httpRequest.getHeader("Authorization").substring(7);
                String refreshToken = body != null ? body.getRefreshToken() : null;

                log.info("Logout request received");
                authService.logout(accessToken, refreshToken);

                ResponseDto<Void> response = new ResponseDto<>();
                response.setStatus(ResponseDto.Status.SUCCESS);
                response.setMessage("Logged out successfully");
                return ResponseEntity.ok(response);
        }
}
