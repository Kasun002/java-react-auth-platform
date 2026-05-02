-- OTP verification tokens for account activation.
-- A user may have multiple records (resends); only the latest unused one is active.
CREATE TABLE otp_verification (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    otp_hash    VARCHAR(64)  NOT NULL,          -- SHA-256 hex of the raw 6-digit OTP
    expires_at  TIMESTAMP    NOT NULL,
    attempts    INT          NOT NULL DEFAULT 0,
    used        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_otp_user_id ON otp_verification(user_id);
