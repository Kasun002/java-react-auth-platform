-- V18: Add DASHBOARD_VIEW permission and assign it to admin roles
-- Idempotent via ON CONFLICT DO NOTHING

INSERT INTO permissions (code, description, category) VALUES
    ('DASHBOARD_VIEW', 'View admin dashboard statistics', 'ADMIN')
ON CONFLICT (code) DO NOTHING;

-- Grant DASHBOARD_VIEW to ROLE_SYSTEM_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM banking_roles r, permissions p
WHERE r.name = 'ROLE_SYSTEM_ADMIN' AND p.code = 'DASHBOARD_VIEW'
ON CONFLICT DO NOTHING;

-- ROLE_SUPER_ADMIN already gets all permissions via bulk insert in V11;
-- re-run the same pattern to cover the new permission
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM banking_roles r, permissions p
WHERE r.name = 'ROLE_SUPER_ADMIN' AND p.code = 'DASHBOARD_VIEW'
ON CONFLICT DO NOTHING;
