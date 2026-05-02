package com.shop.auth.service;

import com.shop.auth.dto.LoginRequestDto;
import com.shop.auth.dto.LoginResponseDto;
import com.shop.auth.dto.RegisterRequestDto;
import com.shop.auth.dto.ResendOtpRequestDto;
import com.shop.auth.dto.VerifyOtpRequestDto;

public interface AuthService {

    void register(RegisterRequestDto request);

    LoginResponseDto login(LoginRequestDto request);

    void verifyOtp(VerifyOtpRequestDto request);

    void resendOtp(ResendOtpRequestDto request);
}
