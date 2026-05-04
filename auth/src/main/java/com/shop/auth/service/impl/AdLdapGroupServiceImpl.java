package com.shop.auth.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.stereotype.Service;

import com.shop.auth.config.AdAuthProperties;
import com.shop.auth.service.AdLdapGroupService;

import lombok.extern.slf4j.Slf4j;

/**
 * Looks up an Azure AD / corporate LDAP directory to find the groups a user
 * belongs to.
 *
 * <p>When LDAP is not configured ({@code app.ad.ldap.url} is blank) the service
 * returns an empty list so the AD login flow continues without group sync.
 *
 * <p>Two-step approach:
 * <ol>
 *   <li>Find the user's DN by searching {@code userSearchBase} with
 *       {@code userSearchFilter} (substituting the user's email).</li>
 *   <li>Find all groups in {@code groupSearchBase} whose membership attribute
 *       ({@code groupSearchFilter}) contains that DN.</li>
 * </ol>
 */
@Slf4j
@Service
public class AdLdapGroupServiceImpl implements AdLdapGroupService {

    private final AdAuthProperties props;
    private final LdapTemplate ldapTemplate;

    public AdLdapGroupServiceImpl(AdAuthProperties props) {
        this.props = props;

        AdAuthProperties.LdapConfig ldap = props.getLdap();
        if (ldap.getUrl() != null && !ldap.getUrl().isBlank()) {
            LdapContextSource contextSource = new LdapContextSource();
            contextSource.setUrl(ldap.getUrl());
            contextSource.setBase(ldap.getBase() != null ? ldap.getBase() : "");
            contextSource.setUserDn(ldap.getUserDn());
            contextSource.setPassword(ldap.getPassword());
            contextSource.afterPropertiesSet();
            this.ldapTemplate = new LdapTemplate(contextSource);
        } else {
            log.info("LDAP URL not configured — LDAP group sync disabled");
            this.ldapTemplate = null;
        }
    }

    @Override
    public List<LdapGroup> getGroupsForUser(String userEmail) {
        if (ldapTemplate == null) {
            return Collections.emptyList();
        }

        try {
            String userDn = resolveUserDn(userEmail);
            if (userDn == null) {
                log.warn("LDAP: no entry found for email=[{}] — skipping group sync", userEmail);
                return Collections.emptyList();
            }

            return findGroupsForDn(userDn);

        } catch (Exception e) {
            log.error("LDAP group lookup failed for email=[{}]: {}", userEmail, e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String resolveUserDn(String userEmail) {
        AdAuthProperties.LdapConfig ldap = props.getLdap();
        String rawFilter = ldap.getUserSearchFilter().replace("{0}", escapeForLdap(userEmail));

        // ContextMapper gives us the full DirContextOperations including the DN
        ContextMapper<String> dnMapper = ctx -> ((DirContextOperations) ctx).getNameInNamespace();
        List<String> dns = ldapTemplate.search(ldap.getUserSearchBase(), rawFilter, dnMapper);
        return dns.isEmpty() ? null : dns.get(0);
    }

    private List<LdapGroup> findGroupsForDn(String userDn) {
        AdAuthProperties.LdapConfig ldap = props.getLdap();
        String rawFilter = ldap.getGroupSearchFilter().replace("{0}", escapeForLdap(userDn));
        String idAttr   = ldap.getGroupIdAttribute();
        String nameAttr = ldap.getGroupNameAttribute();

        AttributesMapper<LdapGroup> mapper = (Attributes attrs) -> {
            try {
                String id   = getAttr(attrs, idAttr);
                String name = getAttr(attrs, nameAttr);
                return (id != null) ? new LdapGroup(id, name != null ? name : id) : null;
            } catch (NamingException e) {
                log.warn("LDAP attribute extraction failed: {}", e.getMessage());
                return null;
            }
        };

        List<LdapGroup> result = ldapTemplate.search(ldap.getGroupSearchBase(), rawFilter, mapper);

        List<LdapGroup> groups = new ArrayList<>();
        for (LdapGroup g : result) {
            if (g != null) groups.add(g);
        }
        log.debug("LDAP found {} group(s) for userDn=[{}]", groups.size(), userDn);
        return groups;
    }

    private String getAttr(Attributes attrs, String name) throws NamingException {
        var attr = attrs.get(name);
        return (attr != null) ? attr.get().toString() : null;
    }

    /** Basic LDAP special-character escaping per RFC 4515. */
    private String escapeForLdap(String value) {
        return value
                .replace("\\", "\\5c")
                .replace("*",  "\\2a")
                .replace("(",  "\\28")
                .replace(")",  "\\29")
                .replace("\0", "\\00");
    }
}
