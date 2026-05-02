package com.shop.auth.dto;

import java.util.ArrayList;
import java.util.List;

import com.shop.auth.utils.Role;
import com.shop.auth.utils.UserStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequestDto {

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be at most 255 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email is not valid format")
    @Size(max = 255, message = "Email must be at most 255 characters")
    private String email;

    @Size(max = 50, message = "Phone number must be at most 50 characters")
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @Valid
    @NotNull(message = "Addresses list is required")
    @Size(min = 1, message = "At least one address is required")
    private List<AddressDto> addresses = new ArrayList<>();

    @NotNull(message = "Status is required")
    private UserStatus status;

    private Role role;
}