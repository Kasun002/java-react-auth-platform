package com.shop.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.auth.dto.RegisterRequestDto;
import com.shop.auth.dto.ResponseDto;
import com.shop.auth.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ResponseDto<Void>> register(@Valid @RequestBody RegisterRequestDto request) {
        ResponseDto<Void> response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    // Add remaining API
}
