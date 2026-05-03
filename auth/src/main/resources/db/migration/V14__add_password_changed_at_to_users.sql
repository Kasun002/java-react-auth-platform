-- Tracks when each user last changed their password.
-- Used to enforce mandatory password rotation (PCI-DSS Req 8.3.9 — max 90 days).
-- Existing rows are set to CURRENT_TIMESTAMP so they get a full 90-day window
-- from the time this migration is deployed.

ALTER TABLE users
    ADD COLUMN password_changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
