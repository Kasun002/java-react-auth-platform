-- V20: Grant PERMISSION_MANAGE to ROLE_SYSTEM_ADMIN.
--
-- V11 assigned ROLE_MANAGE and GROUP_MANAGE to ROLE_SYSTEM_ADMIN but omitted
-- PERMISSION_MANAGE, causing 403 on GET /admin/permissions,
-- POST /admin/roles/{id}/permissions, and DELETE /admin/roles/{id}/permissions/{id}.
-- ROLE_SUPER_ADMIN already holds all permissions via the bulk insert in V11.
--
-- Idempotent via ON CONFLICT DO NOTHING.

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM   banking_roles r,
       permissions   p
WHERE  r.name = 'ROLE_SYSTEM_ADMIN'
  AND  p.code = 'PERMISSION_MANAGE'
ON CONFLICT DO NOTHING;
