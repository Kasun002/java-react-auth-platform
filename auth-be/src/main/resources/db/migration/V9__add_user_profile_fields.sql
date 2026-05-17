-- Optional user profile fields — all nullable so existing rows are unaffected.
-- Used by the profile-edit API when implemented.
ALTER TABLE users
    ADD COLUMN date_of_birth       DATE         NULL,
    ADD COLUMN gender              VARCHAR(50)  NULL,
    ADD COLUMN profile_picture_url VARCHAR(1024) NULL,
    ADD COLUMN last_login_at       TIMESTAMP    NULL;
