-- V25: Add AUDIT_READ permission and grant it to ROLE_SYSTEM_ADMIN and ROLE_SUPER_ADMIN.
--
-- ROLE_SUPER_ADMIN already holds all permissions by convention;
-- ROLE_SYSTEM_ADMIN is the operational admin that should see the audit log.
-- Idempotent via ON CONFLICT DO NOTHING.

INSERT INTO permissions (code, description, category) VALUES
    ('AUDIT_READ', 'View and search the admin audit log (PCI-DSS Req 10.2)', 'Audit & Compliance')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name IN ('ROLE_SYSTEM_ADMIN', 'ROLE_SUPER_ADMIN') AND p.code = 'AUDIT_READ'
ON CONFLICT DO NOTHING;
