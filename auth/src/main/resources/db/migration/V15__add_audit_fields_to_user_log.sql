-- PCI-DSS Req 10.2.4 / 10.2.7 — every login event must record origin IP and device.
-- NULL allowed so existing rows (and non-login token records) are unaffected.

ALTER TABLE user_log
    ADD COLUMN ip_address  VARCHAR(45)  NULL,   -- IPv4 (15) or IPv6 (39) + max with port
    ADD COLUMN user_agent  VARCHAR(512) NULL;    -- browser / SDK identifier
