package com.shop.auth.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "User group with its assigned roles")
public class UserGroupDto {

    @Schema(description = "Group ID", example = "1")
    private Long id;

    @Schema(description = "Group name", example = "RETAIL_CUSTOMER")
    private String name;

    @Schema(description = "Group description", example = "Standard individual account holder")
    private String description;

    @Schema(description = "Group type", example = "CUSTOMER",
            allowableValues = {"CUSTOMER", "STAFF", "OVERSIGHT", "ADMIN"})
    private String type;

    @Schema(description = "Roles assigned to this group")
    private List<BankingRoleDto> roles;
}
