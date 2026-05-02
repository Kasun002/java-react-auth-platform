package com.shop.auth.fixtures;

import com.shop.auth.dto.LoginRequestDto;

public final class LoginRequestDtoFixture {

    private LoginRequestDtoFixture() {}

    public static LoginRequestDto valid() {
        LoginRequestDto dto = new LoginRequestDto();
        dto.setUsername("john.doe@example.com");
        dto.setPassword("Secret@123");
        return dto;
    }

    public static LoginRequestDto withUsername(String username) {
        LoginRequestDto dto = valid();
        dto.setUsername(username);
        return dto;
    }

    public static LoginRequestDto withPassword(String password) {
        LoginRequestDto dto = valid();
        dto.setPassword(password);
        return dto;
    }

    public static LoginRequestDto withNoUsername() {
        LoginRequestDto dto = valid();
        dto.setUsername(null);
        return dto;
    }

    public static LoginRequestDto withNoPassword() {
        LoginRequestDto dto = valid();
        dto.setPassword(null);
        return dto;
    }
}
