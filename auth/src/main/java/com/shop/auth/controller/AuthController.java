package com.shop.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.auth.dto.RegisterRequestDto;
import com.shop.auth.dto.ResponseDto;
import com.shop.auth.service.AuthService;
import com.shop.auth.utils.MaskingUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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

    // Add remaining API
}
