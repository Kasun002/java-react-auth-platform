-- ── V17: Azure AD identity fields on users ────────────────────────────────────
--
-- ad_object_id  — Azure AD Object ID ("oid" claim); stable across app registrations.
--                 Used as the primary key for finding / merging AD users on login.
-- auth_provider — Identifies how the user authenticated when they were created.
--                 LOCAL  = registered via /auth/register (password-based).
--                 AZURE_AD = provisioned on first AD login via /auth/ad/login.

ALTER TABLE users
    ADD COLUMN ad_object_id  VARCHAR(255),
    ADD COLUMN auth_provider VARCHAR(50) NOT NULL DEFAULT 'LOCAL';

-- Partial unique index: only one user per AD Object ID, but allow many NULLs (LOCAL users).
CREATE UNIQUE INDEX idx_users_ad_object_id
    ON users(ad_object_id)
    WHERE ad_object_id IS NOT NULL;
