package com.shop.auth.service;

import com.shop.auth.dto.LoginRequestDto;
import com.shop.auth.dto.LoginResponseDto;
import com.shop.auth.dto.RegisterRequestDto;

public interface AuthService {

    void register(RegisterRequestDto request);

    LoginResponseDto login(LoginRequestDto request);
}
