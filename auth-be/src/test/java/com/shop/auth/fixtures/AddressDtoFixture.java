package com.shop.auth.fixtures;

import com.shop.auth.dto.AddressDto;
import com.shop.auth.utils.LocalStates;

/**
 * Test fixture factory for AddressDto.
 * Provides reusable, named test data across all test classes.
 */
public final class AddressDtoFixture {

    private AddressDtoFixture() {}

    public static AddressDto valid() {
        AddressDto dto = new AddressDto();
        dto.setAddressLine1("123 Main Street");
        dto.setAddressLine2("Apt 4B");
        dto.setStreet("Galle Road");
        dto.setPostalCode("10100");
        dto.setState(LocalStates.WEST.toString());
        dto.setCountry("Sri Lanka");
        return dto;
    }

    public static AddressDto withoutAddressLine1() {
        AddressDto dto = valid();
        dto.setAddressLine1(null);
        return dto;
    }

    public static AddressDto withoutCountry() {
        AddressDto dto = valid();
        dto.setCountry(null);
        return dto;
    }

    public static AddressDto withoutState() {
        AddressDto dto = valid();
        dto.setState(null);
        return dto;
    }
}
