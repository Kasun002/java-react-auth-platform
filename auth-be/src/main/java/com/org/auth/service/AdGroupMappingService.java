package com.org.auth.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.org.auth.dto.AdGroupMappingDto;
import com.org.auth.dto.CreateAdGroupMappingRequestDto;
import com.org.auth.dto.UpdateAdGroupMappingRequestDto;
import com.org.auth.entity.UserGroup;

public interface AdGroupMappingService {

    /**
     * Resolves a list of AD group IDs to local UserGroup entities, applying
     * the configured {@code unmappedGroupStrategy} for any group that has no
     * mapping.
     *
     * @param ldapGroups list of (groupId, groupName) pairs from LDAP lookup
     * @return set of local UserGroup entities the user should be assigned to
     */
    Set<UserGroup> resolveLocalGroups(List<AdLdapGroupService.LdapGroup> ldapGroups);

    /**
     * Returns the configured default local UserGroup (used when LDAP returns no
     * groups for a first-time AD user). Returns empty if the group is not found.
     */
    Optional<UserGroup> getDefaultGroup();

    // ── Admin CRUD ────────────────────────────────────────────────────────────

    List<AdGroupMappingDto> listAll();

    AdGroupMappingDto getById(Long id);

    AdGroupMappingDto create(CreateAdGroupMappingRequestDto request);

    AdGroupMappingDto update(Long id, UpdateAdGroupMappingRequestDto request);

    void delete(Long id);
}
