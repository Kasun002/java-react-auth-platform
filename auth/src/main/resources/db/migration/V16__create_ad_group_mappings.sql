-- ── V16: Azure AD group ↔ local UserGroup mapping table ─────────────────────
--
-- Stores the mapping between an Azure AD group (identified by its Object ID or
-- LDAP CN) and a local UserGroup.  Created automatically by AdAuthServiceImpl
-- when the unmapped-group strategy is AUTO_CREATE; also managed manually via
-- POST /admin/ad/group-mappings.

CREATE TABLE ad_group_mappings (
    id              BIGSERIAL     PRIMARY KEY,

    -- Azure AD Object ID (e.g. "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")
    -- or LDAP CN value when group-source = LDAP.
    ad_group_id     VARCHAR(255)  NOT NULL,

    -- Human-readable display name from Azure AD / LDAP — informational only.
    ad_group_name   VARCHAR(255),

    -- The local UserGroup this AD group is mapped to.
    -- SET NULL if the local group is deleted (avoids cascade-delete of mapping history).
    local_group_id  BIGINT        REFERENCES user_groups(id) ON DELETE SET NULL,

    -- TRUE when this mapping was auto-created by the unmapped-group strategy.
    auto_created    BOOLEAN       NOT NULL DEFAULT FALSE,

    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_ad_group_id UNIQUE (ad_group_id)
);

CREATE INDEX idx_ad_group_mappings_local_group ON ad_group_mappings(local_group_id);
