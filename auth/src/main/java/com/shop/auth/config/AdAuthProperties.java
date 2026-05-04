package com.shop.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * Configuration properties for Azure AD / OIDC token validation and LDAP group lookup.
 *
 * <pre>
 * app.ad.enabled=true
 * app.ad.jwks-uri=https://login.microsoftonline.com/{tenantId}/discovery/v2.0/keys
 * app.ad.issuer=https://login.microsoftonline.com/{tenantId}/v2.0
 * app.ad.audience={clientId}
 * app.ad.unmapped-group-strategy=AUTO_CREATE
 * app.ad.default-group-name=RETAIL_CUSTOMER
 * app.ad.ldap.url=ldap://localhost:389
 * app.ad.ldap.base=dc=corp,dc=example,dc=com
 * app.ad.ldap.user-dn=cn=svc-ldap,ou=service,dc=corp,dc=example,dc=com
 * app.ad.ldap.password=secret
 * app.ad.ldap.group-search-base=ou=groups,dc=corp,dc=example,dc=com
 * app.ad.ldap.group-search-filter=(member={0})
 * app.ad.ldap.group-id-attribute=cn
 * app.ad.ldap.group-name-attribute=cn
 * app.ad.ldap.user-search-base=ou=users,dc=corp,dc=example,dc=com
 * app.ad.ldap.user-search-filter=(mail={0})
 * </pre>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.ad")
public class AdAuthProperties {

    /** Master switch. When false, /auth/ad/login returns 503. */
    private boolean enabled = false;

    /**
     * JWKS endpoint used to verify the ID token signature.
     * Azure AD: https://login.microsoftonline.com/{tenantId}/discovery/v2.0/keys
     * Keycloak: http://localhost:8180/realms/corporate/protocol/openid-connect/certs
     */
    private String jwksUri;

    /**
     * Expected {@code iss} (issuer) claim in the ID token.
     * Azure AD: https://login.microsoftonline.com/{tenantId}/v2.0
     * Keycloak: http://localhost:8180/realms/corporate
     */
    private String issuer;

    /**
     * Expected {@code aud} (audience) claim — typically the client/application ID.
     * The decoder accepts the token only when this value appears in the aud claim.
     */
    private String audience;

    /**
     * Strategy applied when an AD group has no local mapping.
     * AUTO_CREATE — create a new local UserGroup named after the AD group and map it.
     * DEFAULT      — assign the user to the configured defaultGroupName.
     * SKIP         — ignore the unmapped group (user gets no group from it).
     */
    private UnmappedGroupStrategy unmappedGroupStrategy = UnmappedGroupStrategy.DEFAULT;

    /**
     * Name of the local UserGroup used when unmappedGroupStrategy = DEFAULT.
     * Ignored for AUTO_CREATE and SKIP.
     */
    private String defaultGroupName = "RETAIL_CUSTOMER";

    /** LDAP configuration for group lookup. */
    private LdapConfig ldap = new LdapConfig();

    // ── Enums ─────────────────────────────────────────────────────────────────

    public enum UnmappedGroupStrategy {
        AUTO_CREATE,
        DEFAULT,
        SKIP
    }

    // ── Nested config class ───────────────────────────────────────────────────

    @Data
    public static class LdapConfig {

        /** LDAP server URL, e.g. ldap://corp-ldap:389 or ldaps://corp-ldap:636 */
        private String url;

        /** Root DN for all LDAP searches. */
        private String base;

        /** Service-account DN used to bind to LDAP. */
        private String userDn;

        /** Service-account password. */
        private String password;

        /** Base DN under which group entries live. */
        private String groupSearchBase = "ou=groups";

        /**
         * LDAP filter to find the groups a user belongs to.
         * {0} is replaced with the user's full DN.
         * AD: (member={0})  OpenLDAP: (memberUid={0}) or (member={0})
         */
        private String groupSearchFilter = "(member={0})";

        /** LDAP attribute used as the group ID (mapped to adGroupId). */
        private String groupIdAttribute = "cn";

        /** LDAP attribute containing the human-readable group name. */
        private String groupNameAttribute = "cn";

        /** Base DN under which user entries live. */
        private String userSearchBase = "ou=users";

        /**
         * LDAP filter to find a user entry by email / UPN.
         * {0} is replaced with the user's email.
         * AD: (userPrincipalName={0})  OpenLDAP: (mail={0})
         */
        private String userSearchFilter = "(mail={0})";
    }
}
