package com.shop.auth.service;

import com.shop.auth.dto.RegisterRequestDto;

public interface AuthService {
    void register(RegisterRequestDto request);
}
