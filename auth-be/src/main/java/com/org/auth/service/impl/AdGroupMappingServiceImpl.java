package com.org.auth.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.org.auth.exception.ConflictException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.org.auth.config.AdAuthProperties;
import com.org.auth.dto.AdGroupMappingDto;
import com.org.auth.dto.CreateAdGroupMappingRequestDto;
import com.org.auth.dto.UpdateAdGroupMappingRequestDto;
import com.org.auth.entity.AdGroupMapping;
import com.org.auth.entity.UserGroup;
import com.org.auth.exception.ResourceNotFoundException;
import com.org.auth.repository.AdGroupMappingRepository;
import com.org.auth.repository.UserGroupRepository;
import com.org.auth.service.AdGroupMappingService;
import com.org.auth.service.AdLdapGroupService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdGroupMappingServiceImpl implements AdGroupMappingService {

    private final AdAuthProperties props;
    private final AdGroupMappingRepository adGroupMappingRepository;
    private final UserGroupRepository userGroupRepository;

    // ── Core resolution logic ─────────────────────────────────────────────────

    @Override
    @Transactional
    public Set<UserGroup> resolveLocalGroups(List<AdLdapGroupService.LdapGroup> ldapGroups) {
        Set<UserGroup> resolved = new HashSet<>();

        for (AdLdapGroupService.LdapGroup ldapGroup : ldapGroups) {
            Optional<AdGroupMapping> mapping = adGroupMappingRepository.findByAdGroupId(ldapGroup.groupId());

            if (mapping.isPresent() && mapping.get().getLocalGroup() != null) {
                resolved.add(mapping.get().getLocalGroup());
                continue;
            }

            // No mapping found — apply strategy
            AdAuthProperties.UnmappedGroupStrategy strategy = props.getUnmappedGroupStrategy();
            log.debug("No mapping for AD group=[{}] strategy=[{}]", ldapGroup.groupId(), strategy);

            switch (strategy) {
                case AUTO_CREATE -> {
                    UserGroup localGroup = autoCreateAndMap(ldapGroup);
                    resolved.add(localGroup);
                }
                case DEFAULT -> {
                    userGroupRepository.findByName(props.getDefaultGroupName())
                            .ifPresent(resolved::add);
                }
                case SKIP -> log.debug("Skipping unmapped AD group=[{}]", ldapGroup.groupId());
            }
        }

        return resolved;
    }

    // ── Admin CRUD ────────────────────────────────────────────────────────────

    @Override
    public List<AdGroupMappingDto> listAll() {
        return adGroupMappingRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public AdGroupMappingDto getById(Long id) {
        return toDto(findOrThrow(id));
    }

    @Override
    @Transactional
    public AdGroupMappingDto create(CreateAdGroupMappingRequestDto request) {
        if (adGroupMappingRepository.existsByAdGroupId(request.getAdGroupId())) {
            throw new ConflictException(
                    "A mapping for AD group ID '" + request.getAdGroupId() + "' already exists");
        }

        UserGroup localGroup = userGroupRepository.findById(request.getLocalGroupId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "UserGroup", request.getLocalGroupId()));

        AdGroupMapping mapping = new AdGroupMapping();
        mapping.setAdGroupId(request.getAdGroupId());
        mapping.setAdGroupName(request.getAdGroupName());
        mapping.setLocalGroup(localGroup);
        mapping.setAutoCreated(false);

        return toDto(adGroupMappingRepository.save(mapping));
    }

    @Override
    public Optional<UserGroup> getDefaultGroup() {
        return userGroupRepository.findByName(props.getDefaultGroupName());
    }

    @Override
    @Transactional
    public AdGroupMappingDto update(Long id, UpdateAdGroupMappingRequestDto request) {
        AdGroupMapping mapping = findOrThrow(id);

        UserGroup localGroup = userGroupRepository.findById(request.getLocalGroupId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "UserGroup", request.getLocalGroupId()));

        mapping.setLocalGroup(localGroup);
        mapping.setAutoCreated(false); // manual override clears auto-created flag
        return toDto(adGroupMappingRepository.save(mapping));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        adGroupMappingRepository.delete(findOrThrow(id));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private UserGroup autoCreateAndMap(AdLdapGroupService.LdapGroup ldapGroup) {
        // Derive a local group name from the AD group name (sanitise to safe chars)
        String localName = ldapGroup.groupName()
                .toUpperCase()
                .replaceAll("[^A-Z0-9_]", "_")
                .replaceAll("_+", "_");

        // Reuse if already exists
        UserGroup localGroup = userGroupRepository.findByName(localName)
                .orElseGet(() -> {
                    UserGroup g = new UserGroup();
                    g.setName(localName);
                    g.setDescription("Auto-created from AD group: " + ldapGroup.groupName());
                    g.setType("STAFF");
                    log.info("AUTO_CREATE: creating local group=[{}] for AD group=[{}]",
                            localName, ldapGroup.groupId());
                    return userGroupRepository.save(g);
                });

        // Record the mapping
        AdGroupMapping mapping = adGroupMappingRepository
                .findByAdGroupId(ldapGroup.groupId())
                .orElse(new AdGroupMapping());
        mapping.setAdGroupId(ldapGroup.groupId());
        mapping.setAdGroupName(ldapGroup.groupName());
        mapping.setLocalGroup(localGroup);
        mapping.setAutoCreated(true);
        adGroupMappingRepository.save(mapping);

        return localGroup;
    }

    private AdGroupMapping findOrThrow(Long id) {
        return adGroupMappingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AdGroupMapping", id));
    }

    private AdGroupMappingDto toDto(AdGroupMapping m) {
        AdGroupMappingDto dto = new AdGroupMappingDto();
        dto.setId(m.getId());
        dto.setAdGroupId(m.getAdGroupId());
        dto.setAdGroupName(m.getAdGroupName());
        dto.setAutoCreated(m.isAutoCreated());
        dto.setCreatedAt(m.getCreatedAt());
        dto.setUpdatedAt(m.getUpdatedAt());
        if (m.getLocalGroup() != null) {
            dto.setLocalGroupId(m.getLocalGroup().getId());
            dto.setLocalGroupName(m.getLocalGroup().getName());
        }
        return dto;
    }
}
