package com.shop.auth.service;

import java.util.List;
import java.util.Set;

import com.shop.auth.dto.AdGroupMappingDto;
import com.shop.auth.dto.CreateAdGroupMappingRequestDto;
import com.shop.auth.dto.UpdateAdGroupMappingRequestDto;
import com.shop.auth.entity.UserGroup;

public interface AdGroupMappingService {

    /**
     * Resolves a list of AD group IDs to local UserGroup entities, applying
     * the configured {@code unmappedGroupStrategy} for any group that has no mapping.
     *
     * @param ldapGroups list of (groupId, groupName) pairs from LDAP lookup
     * @return set of local UserGroup entities the user should be assigned to
     */
    Set<UserGroup> resolveLocalGroups(List<AdLdapGroupService.LdapGroup> ldapGroups);

    // ── Admin CRUD ────────────────────────────────────────────────────────────

    List<AdGroupMappingDto> listAll();

    AdGroupMappingDto getById(Long id);

    AdGroupMappingDto create(CreateAdGroupMappingRequestDto request);

    AdGroupMappingDto update(Long id, UpdateAdGroupMappingRequestDto request);

    void delete(Long id);
}
