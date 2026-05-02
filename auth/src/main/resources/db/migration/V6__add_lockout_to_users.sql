-- Support brute-force lockout: track failed login attempts and lockout expiry.
ALTER TABLE users
    ADD COLUMN failed_login_attempts INT       NOT NULL DEFAULT 0,
    ADD COLUMN locked_until          TIMESTAMP NULL;
