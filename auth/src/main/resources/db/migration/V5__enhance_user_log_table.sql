-- Extend user_log table to support full login-token audit trail.
-- V3 only created id + user_token; this migration adds the remaining columns.

ALTER TABLE user_log ALTER COLUMN user_token TYPE VARCHAR(512);
ALTER TABLE user_log ALTER COLUMN user_token SET NOT NULL;

ALTER TABLE user_log
    ADD COLUMN user_id      BIGINT      REFERENCES users(id) ON DELETE CASCADE,
    ADD COLUMN token_type   VARCHAR(20) NOT NULL DEFAULT 'ACCESS',
    ADD COLUMN issued_at    TIMESTAMP   NOT NULL DEFAULT NOW(),
    ADD COLUMN expires_at   TIMESTAMP   NOT NULL DEFAULT NOW(),
    ADD COLUMN created_at   TIMESTAMP   NOT NULL DEFAULT NOW(),
    ADD COLUMN updated_at   TIMESTAMP   NOT NULL DEFAULT NOW();
