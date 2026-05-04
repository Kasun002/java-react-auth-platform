package com.shop.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request to manually map an Azure AD group to a local UserGroup")
public class CreateAdGroupMappingRequestDto {

    @NotBlank
    @Schema(description = "Azure AD Object ID or LDAP CN of the AD group",
            example = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")
    private String adGroupId;

    @Schema(description = "Display name of the AD group (informational only)",
            example = "GRP-RETAIL-CUSTOMERS")
    private String adGroupName;

    @NotNull
    @Schema(description = "ID of the local UserGroup to map to", example = "3")
    private Long localGroupId;
}
