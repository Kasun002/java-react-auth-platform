package com.shop.auth.dto;

import com.shop.auth.utils.LocalStates;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddressDto {
    @NotBlank(message = "Address line one is required")
    private String addressLine1;

    private String addressLine2;

    private String street;

    private String postalCode;

    private LocalStates state;

    @NotBlank(message = "Country is required")
    private String country;
}
