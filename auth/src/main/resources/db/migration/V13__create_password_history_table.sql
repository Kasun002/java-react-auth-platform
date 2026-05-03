-- Password history — stores BCrypt hashes of previous passwords per user.
-- Used to enforce the banking requirement that users cannot reuse any of
-- their last N passwords (PCI-DSS Req 8.3.6 / NIST 800-63B §5.1.1).

CREATE TABLE password_history (
    id            BIGSERIAL    PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    password_hash VARCHAR(60)  NOT NULL,    -- BCrypt hash is always 60 chars
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Queries always filter by user and order by date to find the N most recent entries
CREATE INDEX idx_password_history_user_created ON password_history (user_id, created_at DESC);
