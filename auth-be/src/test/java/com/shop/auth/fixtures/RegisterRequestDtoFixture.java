package com.shop.auth.fixtures;

import java.util.ArrayList;
import java.util.List;

import com.shop.auth.dto.AddressDto;
import com.shop.auth.dto.RegisterRequestDto;

/**
 * Test fixture factory for RegisterRequestDto.
 * All variants start from a valid baseline — only the relevant field is mutated.
 */
public final class RegisterRequestDtoFixture {

    private RegisterRequestDtoFixture() {}

    /** Fully valid registration request — passes all validation constraints. */
    public static RegisterRequestDto valid() {
        RegisterRequestDto dto = new RegisterRequestDto();
        dto.setName("John Doe");
        dto.setEmail("john.doe@example.com");
        dto.setPhone("+94771234567");
        dto.setPassword("Secret@Pass1!");
        dto.setAddresses(new ArrayList<>(List.of(AddressDtoFixture.valid())));
        return dto;
    }

    public static RegisterRequestDto withEmail(String email) {
        RegisterRequestDto dto = valid();
        dto.setEmail(email);
        return dto;
    }

    public static RegisterRequestDto withPassword(String password) {
        RegisterRequestDto dto = valid();
        dto.setPassword(password);
        return dto;
    }

    public static RegisterRequestDto withAddresses(List<AddressDto> addresses) {
        RegisterRequestDto dto = valid();
        dto.setAddresses(addresses);
        return dto;
    }

    public static RegisterRequestDto withNoName() {
        RegisterRequestDto dto = valid();
        dto.setName(null);
        return dto;
    }

    public static RegisterRequestDto withNoEmail() {
        RegisterRequestDto dto = valid();
        dto.setEmail(null);
        return dto;
    }
}
