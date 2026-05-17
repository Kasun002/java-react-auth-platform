-- V24: PCI-DSS Req 10.2 — persistent audit trail for every admin action.
--
-- status is stored as VARCHAR with a CHECK constraint so it is human-readable
-- in raw SQL and does not require a Postgres ENUM type (avoids ALTER TYPE pain).

CREATE TABLE audit_log (
    id          BIGSERIAL       PRIMARY KEY,
    actor_id    BIGINT          NOT NULL REFERENCES users(id),
    actor_name  VARCHAR(255)    NOT NULL,
    action      VARCHAR(100)    NOT NULL,
    resource    VARCHAR(100)    NOT NULL,
    resource_id VARCHAR(255),
    details     TEXT,
    ip_address  VARCHAR(45),
    status      VARCHAR(10)     NOT NULL DEFAULT 'SUCCESS'
                    CHECK (status IN ('SUCCESS', 'WARNING', 'FAILURE')),
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_log_created_at ON audit_log (created_at DESC);
CREATE INDEX idx_audit_log_status     ON audit_log (status);
CREATE INDEX idx_audit_log_actor_id   ON audit_log (actor_id);
