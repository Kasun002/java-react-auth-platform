package com.shop.auth.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Azure AD group to local UserGroup mapping")
public class AdGroupMappingDto {

    @Schema(description = "Mapping ID", example = "1")
    private Long id;

    @Schema(description = "Azure AD Object ID or LDAP CN of the AD group",
            example = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")
    private String adGroupId;

    @Schema(description = "Display name of the AD group", example = "GRP-RETAIL-CUSTOMERS")
    private String adGroupName;

    @Schema(description = "ID of the mapped local UserGroup", example = "3")
    private Long localGroupId;

    @Schema(description = "Name of the mapped local UserGroup", example = "RETAIL_CUSTOMER")
    private String localGroupName;

    @Schema(description = "True when this mapping was auto-created", example = "false")
    private boolean autoCreated;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}
