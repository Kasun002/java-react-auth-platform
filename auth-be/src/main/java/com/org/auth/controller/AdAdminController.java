package com.org.auth.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.org.auth.dto.AdGroupMappingDto;
import com.org.auth.dto.CreateAdGroupMappingRequestDto;
import com.org.auth.dto.ResponseDto;
import com.org.auth.dto.UpdateAdGroupMappingRequestDto;
import com.org.auth.service.AdGroupMappingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Admin API for managing Azure AD ↔ local UserGroup mappings.
 *
 * <p>
 * Requires the {@code AD_GROUP_MANAGE} permission on every endpoint.
 * Mappings control how AD groups (identified by their Object ID or LDAP CN)
 * translate to local UserGroup entities during AD login.
 */
@Tag(name = "AD Admin", description = "Manage Azure AD group ↔ local UserGroup mappings")
@RestController
@RequestMapping("/admin/ad/group-mappings")
@RequiredArgsConstructor
public class AdAdminController {

        private final AdGroupMappingService adGroupMappingService;

        @Operation(summary = "List all AD group mappings")
        @GetMapping
        @PreAuthorize("hasAuthority('AD_GROUP_MANAGE')")
        public ResponseEntity<ResponseDto<List<AdGroupMappingDto>>> listAll() {
                ResponseDto<List<AdGroupMappingDto>> response = new ResponseDto<>();
                response.setStatus(ResponseDto.Status.SUCCESS);
                response.setData(adGroupMappingService.listAll());
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Get a single AD group mapping by ID")
        @ApiResponse(responseCode = "200", description = "Mapping found")
        @ApiResponse(responseCode = "404", description = "Mapping not found")
        @GetMapping("/{id}")
        @PreAuthorize("hasAuthority('AD_GROUP_MANAGE')")
        public ResponseEntity<ResponseDto<AdGroupMappingDto>> getById(@PathVariable Long id) {
                ResponseDto<AdGroupMappingDto> response = new ResponseDto<>();
                response.setStatus(ResponseDto.Status.SUCCESS);
                response.setData(adGroupMappingService.getById(id));
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Create a manual AD group mapping", description = "Maps an Azure AD group (by its Object ID or LDAP CN) to a local UserGroup. "
                        + "This overrides any auto-created mapping for the same AD group.")
        @ApiResponse(responseCode = "201", description = "Mapping created")
        @ApiResponse(responseCode = "400", description = "Validation error")
        @ApiResponse(responseCode = "404", description = "Local UserGroup not found")
        @PostMapping
        @PreAuthorize("hasAuthority('AD_GROUP_MANAGE')")
        public ResponseEntity<ResponseDto<AdGroupMappingDto>> create(
                        @Valid @RequestBody CreateAdGroupMappingRequestDto request) {
                ResponseDto<AdGroupMappingDto> response = new ResponseDto<>();
                response.setStatus(ResponseDto.Status.SUCCESS);
                response.setMessage("AD group mapping created");
                response.setData(adGroupMappingService.create(request));
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @Operation(summary = "Update the local group for an existing AD mapping", description = "Changes which local UserGroup an AD group maps to. "
                        +
                        "Automatically clears the auto-created flag.")
        @ApiResponse(responseCode = "200", description = "Mapping updated")
        @ApiResponse(responseCode = "404", description = "Mapping or local UserGroup not found")
        @PutMapping("/{id}")
        @PreAuthorize("hasAuthority('AD_GROUP_MANAGE')")
        public ResponseEntity<ResponseDto<AdGroupMappingDto>> update(
                        @PathVariable Long id,
                        @Valid @RequestBody UpdateAdGroupMappingRequestDto request) {
                ResponseDto<AdGroupMappingDto> response = new ResponseDto<>();
                response.setStatus(ResponseDto.Status.SUCCESS);
                response.setMessage("AD group mapping updated");
                response.setData(adGroupMappingService.update(id, request));
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Delete an AD group mapping")
        @ApiResponse(responseCode = "204", description = "Mapping deleted")
        @ApiResponse(responseCode = "404", description = "Mapping not found")
        @DeleteMapping("/{id}")
        @PreAuthorize("hasAuthority('AD_GROUP_MANAGE')")
        public ResponseEntity<Void> delete(@PathVariable Long id) {
                adGroupMappingService.delete(id);
                return ResponseEntity.noContent().build();
        }
}
