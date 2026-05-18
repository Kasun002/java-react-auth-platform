package com.org.auth.service;

import java.util.List;

/**
 * Retrieves the list of LDAP groups a user belongs to.
 *
 * <p>When LDAP is not configured ({@code app.ad.ldap.url} is blank), implementations
 * must return an empty list instead of throwing — the AD login flow continues
 * without group sync.
 */
public interface AdLdapGroupService {

    /** Immutable descriptor for a single LDAP / AD group. */
    record LdapGroup(String groupId, String groupName) {}

    /**
     * Returns the AD / LDAP groups that the given user (identified by email / UPN)
     * belongs to.
     *
     * @param userEmail the user's email address or UPN
     * @return list of LDAP group descriptors; never null; may be empty
     */
    List<LdapGroup> getGroupsForUser(String userEmail);
}
