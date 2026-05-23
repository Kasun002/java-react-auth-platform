package com.org.auth.service.impl;

import com.org.auth.config.AdAuthProperties;
import com.org.auth.config.AdAuthProperties.UnmappedGroupStrategy;
import com.org.auth.dto.AdGroupMappingDto;
import com.org.auth.dto.CreateAdGroupMappingRequestDto;
import com.org.auth.dto.UpdateAdGroupMappingRequestDto;
import com.org.auth.entity.AdGroupMapping;
import com.org.auth.entity.UserGroup;
import com.org.auth.exception.ConflictException;
import com.org.auth.exception.ResourceNotFoundException;
import com.org.auth.repository.AdGroupMappingRepository;
import com.org.auth.repository.UserGroupRepository;
import com.org.auth.service.AdLdapGroupService.LdapGroup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AdGroupMappingServiceImpl}.
 *
 * <p>Covers:
 * <ul>
 *   <li>{@code resolveLocalGroups()} — all three unmapped-group strategies.</li>
 *   <li>{@code getDefaultGroup()} — delegates to configured name lookup.</li>
 *   <li>Admin CRUD: listAll, getById, create (duplicate guard), update, delete.</li>
 * </ul>
 *
 * <p>No Spring context — all collaborators are Mockito mocks.
 * STRICT_STUBS surfaces unused stubs immediately.
 */
@DisplayName("AdGroupMappingServiceImpl")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class AdGroupMappingServiceImplTest {

    @Mock private AdAuthProperties         props;
    @Mock private AdGroupMappingRepository adGroupMappingRepository;
    @Mock private UserGroupRepository      userGroupRepository;

    @InjectMocks private AdGroupMappingServiceImpl service;

    // ── Entity / DTO helpers ──────────────────────────────────────────────────

    private UserGroup makeGroup(Long id, String name) {
        UserGroup g = new UserGroup();
        g.setId(id);
        g.setName(name);
        g.setType("STAFF");
        return g;
    }

    private AdGroupMapping makeMapping(Long id, String adGroupId, String adGroupName, UserGroup localGroup) {
        AdGroupMapping m = new AdGroupMapping();
        m.setId(id);
        m.setAdGroupId(adGroupId);
        m.setAdGroupName(adGroupName);
        m.setLocalGroup(localGroup);
        m.setAutoCreated(false);
        return m;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // resolveLocalGroups()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("resolveLocalGroups()")
    class ResolveLocalGroups {

        @Test
        @DisplayName("Returns empty Set when ldapGroups list is empty")
        void shouldReturnEmptySetForEmptyInput() {
            Set<UserGroup> result = service.resolveLocalGroups(List.of());
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Returns the mapped local UserGroup when an exact AD group mapping exists")
        void shouldReturnMappedLocalGroup() {
            UserGroup local = makeGroup(1L, "RETAIL_CUSTOMER");
            AdGroupMapping mapping = makeMapping(10L, "ad-gid-001", "GRP-RETAIL", local);

            when(adGroupMappingRepository.findByAdGroupId("ad-gid-001"))
                    .thenReturn(Optional.of(mapping));

            Set<UserGroup> result = service.resolveLocalGroups(
                    List.of(new LdapGroup("ad-gid-001", "GRP-RETAIL")));

            assertThat(result).containsExactly(local);
        }

        @Test
        @DisplayName("Skips a mapping whose localGroup was deleted (FK = NULL) and applies strategy instead")
        void shouldApplyStrategyWhenMappedLocalGroupIsNull() {
            AdGroupMapping nullGroupMapping = makeMapping(10L, "ad-gid-001", "GRP-X", null);
            UserGroup defaultGroup = makeGroup(99L, "DEFAULT_GROUP");

            when(adGroupMappingRepository.findByAdGroupId("ad-gid-001"))
                    .thenReturn(Optional.of(nullGroupMapping));
            when(props.getUnmappedGroupStrategy()).thenReturn(UnmappedGroupStrategy.DEFAULT);
            when(props.getDefaultGroupName()).thenReturn("DEFAULT_GROUP");
            when(userGroupRepository.findByName("DEFAULT_GROUP")).thenReturn(Optional.of(defaultGroup));

            Set<UserGroup> result = service.resolveLocalGroups(
                    List.of(new LdapGroup("ad-gid-001", "GRP-X")));

            assertThat(result).containsExactly(defaultGroup);
        }

        @Test
        @DisplayName("Deduplicates: two LDAP groups mapped to the same local group produce one entry")
        void shouldDeduplicateWhenMultipleLdapGroupsMappedToSameLocalGroup() {
            UserGroup shared = makeGroup(1L, "SHARED");
            AdGroupMapping m1 = makeMapping(10L, "ad-gid-A", "Group A", shared);
            AdGroupMapping m2 = makeMapping(11L, "ad-gid-B", "Group B", shared);

            when(adGroupMappingRepository.findByAdGroupId("ad-gid-A")).thenReturn(Optional.of(m1));
            when(adGroupMappingRepository.findByAdGroupId("ad-gid-B")).thenReturn(Optional.of(m2));

            Set<UserGroup> result = service.resolveLocalGroups(
                    List.of(new LdapGroup("ad-gid-A", "Group A"),
                            new LdapGroup("ad-gid-B", "Group B")));

            assertThat(result).hasSize(1).containsExactly(shared);
        }

        // ── Strategy: AUTO_CREATE ─────────────────────────────────────────────

        @Nested
        @DisplayName("Strategy: AUTO_CREATE")
        class AutoCreate {

            @Test
            @DisplayName("Creates a new local UserGroup and mapping when no prior mapping exists")
            void shouldCreateGroupAndMappingForUnmappedAdGroup() {
                when(adGroupMappingRepository.findByAdGroupId("ad-gid-new"))
                        .thenReturn(Optional.empty());
                when(props.getUnmappedGroupStrategy()).thenReturn(UnmappedGroupStrategy.AUTO_CREATE);

                String expectedLocalName = "CORP_ENGINEERS";
                when(userGroupRepository.findByName(expectedLocalName)).thenReturn(Optional.empty());

                UserGroup created = makeGroup(50L, expectedLocalName);
                when(userGroupRepository.save(argThat(g -> expectedLocalName.equals(g.getName()))))
                        .thenReturn(created);
                when(adGroupMappingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                Set<UserGroup> result = service.resolveLocalGroups(
                        List.of(new LdapGroup("ad-gid-new", "corp engineers")));

                assertThat(result).containsExactly(created);
                verify(userGroupRepository).save(argThat(g ->
                        expectedLocalName.equals(g.getName()) && "STAFF".equals(g.getType())));
            }

            @Test
            @DisplayName("Reuses an existing local group by sanitised name and records mapping (no duplicate group)")
            void shouldReuseExistingGroupBySanitisedName() {
                when(adGroupMappingRepository.findByAdGroupId("ad-gid-existing"))
                        .thenReturn(Optional.empty());
                when(props.getUnmappedGroupStrategy()).thenReturn(UnmappedGroupStrategy.AUTO_CREATE);

                UserGroup existing = makeGroup(10L, "CORP_ENGINEERS");
                when(userGroupRepository.findByName("CORP_ENGINEERS")).thenReturn(Optional.of(existing));
                when(adGroupMappingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                Set<UserGroup> result = service.resolveLocalGroups(
                        List.of(new LdapGroup("ad-gid-existing", "corp engineers")));

                assertThat(result).containsExactly(existing);
                // Group must NOT be created again
                verify(userGroupRepository, never()).save(any());
            }

            @ParameterizedTest(name = "AD group name \"{0}\" → sanitised to uppercase+underscores")
            @DisplayName("Sanitises special characters in AD group name to [A-Z0-9_]")
            @ValueSource(strings = {
                    "corp-engineers",
                    "Corp Engineers!",
                    "Corp/Engineering/2024",
                    "corp--double--dash"
            })
            void shouldSanitiseAdGroupNameToUppercaseUnderscores(String adGroupName) {
                when(adGroupMappingRepository.findByAdGroupId("ad-gid-x"))
                        .thenReturn(Optional.empty());
                when(props.getUnmappedGroupStrategy()).thenReturn(UnmappedGroupStrategy.AUTO_CREATE);

                when(userGroupRepository.findByName(any())).thenReturn(Optional.empty());
                when(userGroupRepository.save(any())).thenAnswer(inv -> {
                    UserGroup g = inv.getArgument(0);
                    // Verify name contains only valid characters
                    assertThat(g.getName()).matches("[A-Z0-9_]+");
                    return g;
                });
                when(adGroupMappingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                service.resolveLocalGroups(List.of(new LdapGroup("ad-gid-x", adGroupName)));

                verify(userGroupRepository).save(any());
            }

            @Test
            @DisplayName("Sets autoCreated=true on the mapping record")
            void shouldMarkMappingAsAutoCreated() {
                when(adGroupMappingRepository.findByAdGroupId("ad-gid-new2"))
                        .thenReturn(Optional.empty());
                when(props.getUnmappedGroupStrategy()).thenReturn(UnmappedGroupStrategy.AUTO_CREATE);
                when(userGroupRepository.findByName(any())).thenReturn(Optional.empty());
                when(userGroupRepository.save(any())).thenAnswer(inv -> {
                    UserGroup g = inv.getArgument(0);
                    g.setId(55L);
                    return g;
                });

                ArgumentCaptor<AdGroupMapping> mappingCaptor =
                        ArgumentCaptor.forClass(AdGroupMapping.class);
                when(adGroupMappingRepository.save(mappingCaptor.capture()))
                        .thenAnswer(inv -> inv.getArgument(0));

                service.resolveLocalGroups(List.of(new LdapGroup("ad-gid-new2", "New Group")));

                assertThat(mappingCaptor.getValue().isAutoCreated()).isTrue();
            }
        }

        // ── Strategy: DEFAULT ─────────────────────────────────────────────────

        @Nested
        @DisplayName("Strategy: DEFAULT")
        class StrategyDefault {

            @Test
            @DisplayName("Assigns the configured default group when it exists")
            void shouldAssignDefaultGroupWhenItExists() {
                UserGroup defaultGroup = makeGroup(99L, "RETAIL_CUSTOMER");

                when(adGroupMappingRepository.findByAdGroupId("unmapped-id"))
                        .thenReturn(Optional.empty());
                when(props.getUnmappedGroupStrategy()).thenReturn(UnmappedGroupStrategy.DEFAULT);
                when(props.getDefaultGroupName()).thenReturn("RETAIL_CUSTOMER");
                when(userGroupRepository.findByName("RETAIL_CUSTOMER"))
                        .thenReturn(Optional.of(defaultGroup));

                Set<UserGroup> result = service.resolveLocalGroups(
                        List.of(new LdapGroup("unmapped-id", "Some AD Group")));

                assertThat(result).containsExactly(defaultGroup);
            }

            @Test
            @DisplayName("Returns empty Set when default group is not configured in the database")
            void shouldReturnEmptyWhenDefaultGroupNotFound() {
                when(adGroupMappingRepository.findByAdGroupId("unmapped-id"))
                        .thenReturn(Optional.empty());
                when(props.getUnmappedGroupStrategy()).thenReturn(UnmappedGroupStrategy.DEFAULT);
                when(props.getDefaultGroupName()).thenReturn("NONEXISTENT_GROUP");
                when(userGroupRepository.findByName("NONEXISTENT_GROUP"))
                        .thenReturn(Optional.empty());

                Set<UserGroup> result = service.resolveLocalGroups(
                        List.of(new LdapGroup("unmapped-id", "Some AD Group")));

                assertThat(result).isEmpty();
            }
        }

        // ── Strategy: SKIP ────────────────────────────────────────────────────

        @Nested
        @DisplayName("Strategy: SKIP")
        class StrategySkip {

            @Test
            @DisplayName("Returns empty Set — unmapped AD groups are silently discarded")
            void shouldReturnEmptySetForUnmappedGroup() {
                when(adGroupMappingRepository.findByAdGroupId("skip-gid"))
                        .thenReturn(Optional.empty());
                when(props.getUnmappedGroupStrategy()).thenReturn(UnmappedGroupStrategy.SKIP);

                Set<UserGroup> result = service.resolveLocalGroups(
                        List.of(new LdapGroup("skip-gid", "Skip Me")));

                assertThat(result).isEmpty();
                verify(userGroupRepository, never()).save(any());
                verify(userGroupRepository, never()).findByName(any());
            }

            @Test
            @DisplayName("Mixed input: mapped group is included; unmapped group is skipped")
            void shouldIncludeMappedAndSkipUnmapped() {
                UserGroup local = makeGroup(1L, "INCLUDED");
                AdGroupMapping mapping = makeMapping(10L, "mapped-gid", "Mapped Group", local);

                when(adGroupMappingRepository.findByAdGroupId("mapped-gid"))
                        .thenReturn(Optional.of(mapping));
                when(adGroupMappingRepository.findByAdGroupId("skip-gid"))
                        .thenReturn(Optional.empty());
                when(props.getUnmappedGroupStrategy()).thenReturn(UnmappedGroupStrategy.SKIP);

                Set<UserGroup> result = service.resolveLocalGroups(List.of(
                        new LdapGroup("mapped-gid", "Mapped Group"),
                        new LdapGroup("skip-gid",   "Skip Me")));

                assertThat(result).containsExactly(local);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // getDefaultGroup()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getDefaultGroup()")
    class GetDefaultGroup {

        @Test
        @DisplayName("Returns the UserGroup matching the configured defaultGroupName")
        void shouldReturnDefaultGroup() {
            UserGroup defaultGroup = makeGroup(1L, "RETAIL_CUSTOMER");
            when(props.getDefaultGroupName()).thenReturn("RETAIL_CUSTOMER");
            when(userGroupRepository.findByName("RETAIL_CUSTOMER")).thenReturn(Optional.of(defaultGroup));

            assertThat(service.getDefaultGroup()).contains(defaultGroup);
        }

        @Test
        @DisplayName("Returns Optional.empty() when defaultGroupName is not present in the database")
        void shouldReturnEmptyWhenDefaultGroupNotFound() {
            when(props.getDefaultGroupName()).thenReturn("MISSING_GROUP");
            when(userGroupRepository.findByName("MISSING_GROUP")).thenReturn(Optional.empty());

            assertThat(service.getDefaultGroup()).isEmpty();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Admin CRUD — listAll()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("listAll()")
    class ListAll {

        @Test
        @DisplayName("Returns empty list when no mappings exist")
        void shouldReturnEmptyList() {
            when(adGroupMappingRepository.findAll()).thenReturn(List.of());
            assertThat(service.listAll()).isEmpty();
        }

        @Test
        @DisplayName("Returns a DTO for every mapping including localGroup details")
        void shouldReturnDtoForEachMapping() {
            UserGroup g = makeGroup(3L, "RETAIL_CUSTOMER");
            AdGroupMapping m = makeMapping(1L, "ad-gid-001", "GRP-RETAIL", g);

            when(adGroupMappingRepository.findAll()).thenReturn(List.of(m));

            List<AdGroupMappingDto> result = service.listAll();

            assertThat(result).hasSize(1);
            AdGroupMappingDto dto = result.get(0);
            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getAdGroupId()).isEqualTo("ad-gid-001");
            assertThat(dto.getAdGroupName()).isEqualTo("GRP-RETAIL");
            assertThat(dto.getLocalGroupId()).isEqualTo(3L);
            assertThat(dto.getLocalGroupName()).isEqualTo("RETAIL_CUSTOMER");
            assertThat(dto.isAutoCreated()).isFalse();
        }

        @Test
        @DisplayName("Returns DTO with null localGroupId and localGroupName when localGroup is null (deleted FK)")
        void shouldReturnNullLocalGroupFieldsWhenLocalGroupIsNull() {
            AdGroupMapping m = makeMapping(2L, "ad-gid-orphan", "Orphan Group", null);
            when(adGroupMappingRepository.findAll()).thenReturn(List.of(m));

            AdGroupMappingDto dto = service.listAll().get(0);

            assertThat(dto.getLocalGroupId()).isNull();
            assertThat(dto.getLocalGroupName()).isNull();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Admin CRUD — getById()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getById()")
    class GetById {

        @Test
        @DisplayName("Returns DTO when mapping exists")
        void shouldReturnDtoWhenFound() {
            UserGroup g = makeGroup(1L, "STAFF_GROUP");
            AdGroupMapping m = makeMapping(10L, "ad-gid-x", "AD Group X", g);

            when(adGroupMappingRepository.findById(10L)).thenReturn(Optional.of(m));

            AdGroupMappingDto dto = service.getById(10L);

            assertThat(dto.getId()).isEqualTo(10L);
            assertThat(dto.getAdGroupId()).isEqualTo("ad-gid-x");
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when mapping does not exist")
        void shouldThrowWhenNotFound() {
            when(adGroupMappingRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Admin CRUD — create()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("create()")
    class Create {

        private CreateAdGroupMappingRequestDto buildRequest() {
            CreateAdGroupMappingRequestDto req = new CreateAdGroupMappingRequestDto();
            req.setAdGroupId("ad-gid-new");
            req.setAdGroupName("New AD Group");
            req.setLocalGroupId(5L);
            return req;
        }

        @Test
        @DisplayName("Creates and returns a new mapping DTO when adGroupId is unique")
        void shouldCreateMappingSuccessfully() {
            UserGroup local = makeGroup(5L, "TARGET_GROUP");
            AdGroupMapping saved = makeMapping(20L, "ad-gid-new", "New AD Group", local);

            when(adGroupMappingRepository.existsByAdGroupId("ad-gid-new")).thenReturn(false);
            when(userGroupRepository.findById(5L)).thenReturn(Optional.of(local));
            when(adGroupMappingRepository.save(any())).thenReturn(saved);

            AdGroupMappingDto result = service.create(buildRequest());

            assertThat(result.getId()).isEqualTo(20L);
            assertThat(result.getAdGroupId()).isEqualTo("ad-gid-new");
            assertThat(result.getLocalGroupName()).isEqualTo("TARGET_GROUP");
            assertThat(result.isAutoCreated()).isFalse();
        }

        @Test
        @DisplayName("Throws ConflictException (409) when a mapping for the same adGroupId already exists")
        void shouldThrowConflictWhenDuplicateAdGroupId() {
            when(adGroupMappingRepository.existsByAdGroupId("ad-gid-new")).thenReturn(true);

            assertThatThrownBy(() -> service.create(buildRequest()))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("ad-gid-new");
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when the target local UserGroup does not exist")
        void shouldThrowWhenLocalGroupNotFound() {
            when(adGroupMappingRepository.existsByAdGroupId("ad-gid-new")).thenReturn(false);
            when(userGroupRepository.findById(5L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(buildRequest()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Persisted mapping has autoCreated=false for manually created entries")
        void shouldPersistAutoCreatedFalse() {
            UserGroup local = makeGroup(5L, "TARGET_GROUP");
            ArgumentCaptor<AdGroupMapping> captor = ArgumentCaptor.forClass(AdGroupMapping.class);

            when(adGroupMappingRepository.existsByAdGroupId("ad-gid-new")).thenReturn(false);
            when(userGroupRepository.findById(5L)).thenReturn(Optional.of(local));
            when(adGroupMappingRepository.save(captor.capture()))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.create(buildRequest());

            assertThat(captor.getValue().isAutoCreated()).isFalse();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Admin CRUD — update()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("update()")
    class Update {

        private UpdateAdGroupMappingRequestDto buildRequest(Long localGroupId) {
            UpdateAdGroupMappingRequestDto req = new UpdateAdGroupMappingRequestDto();
            req.setLocalGroupId(localGroupId);
            return req;
        }

        @Test
        @DisplayName("Updates localGroup and clears autoCreated flag on success")
        void shouldUpdateLocalGroupSuccessfully() {
            UserGroup oldGroup = makeGroup(1L, "OLD_GROUP");
            UserGroup newGroup = makeGroup(2L, "NEW_GROUP");
            AdGroupMapping existing = makeMapping(10L, "ad-gid-x", "AD X", oldGroup);
            existing.setAutoCreated(true); // was auto-created, manual update should clear it

            when(adGroupMappingRepository.findById(10L)).thenReturn(Optional.of(existing));
            when(userGroupRepository.findById(2L)).thenReturn(Optional.of(newGroup));
            when(adGroupMappingRepository.save(existing)).thenReturn(existing);

            AdGroupMappingDto result = service.update(10L, buildRequest(2L));

            assertThat(result.getLocalGroupName()).isEqualTo("NEW_GROUP");
            assertThat(existing.isAutoCreated()).isFalse();
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when the mapping to update does not exist")
        void shouldThrowWhenMappingNotFound() {
            when(adGroupMappingRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(99L, buildRequest(1L)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when the new local UserGroup does not exist")
        void shouldThrowWhenNewLocalGroupNotFound() {
            AdGroupMapping existing = makeMapping(10L, "ad-gid-x", "AD X", makeGroup(1L, "OLD"));

            when(adGroupMappingRepository.findById(10L)).thenReturn(Optional.of(existing));
            when(userGroupRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(10L, buildRequest(999L)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Admin CRUD — delete()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("Deletes the mapping when it exists")
        void shouldDeleteSuccessfully() {
            AdGroupMapping m = makeMapping(10L, "ad-gid-x", "AD X", makeGroup(1L, "GROUP"));

            when(adGroupMappingRepository.findById(10L)).thenReturn(Optional.of(m));

            service.delete(10L);

            verify(adGroupMappingRepository).delete(m);
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when the mapping does not exist")
        void shouldThrowWhenNotFound() {
            when(adGroupMappingRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(99L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(adGroupMappingRepository, never()).delete(any());
        }
    }
}
