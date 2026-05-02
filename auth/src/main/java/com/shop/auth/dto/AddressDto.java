package com.shop.auth.dto;

import com.shop.auth.utils.LocalStates;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Address details for a user")
public class AddressDto {

    @Schema(description = "Primary address line", example = "123 Main Street", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Address line one is required")
    private String addressLine1;

    @Schema(description = "Secondary address line", example = "Apt 4B")
    private String addressLine2;

    @Schema(description = "Street name", example = "Galle Road")
    private String street;

    @Schema(description = "Postal / ZIP code", example = "10100")
    private String postalCode;

    @Schema(description = "Province or state", example = "WEST")
    private LocalStates state;

    @Schema(description = "Country name", example = "Sri Lanka", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Country is required")
    private String country;
}
