package com.shop.auth.service;

import com.shop.auth.dto.RegisterRequestDto;
import com.shop.auth.dto.ResponseDto;

public interface AuthService {
    ResponseDto<Void> register(RegisterRequestDto request);
}
