-- V23: Drop the legacy users.role column introduced in V1.
--
-- The binary USER/ADMIN role enum is superseded by the full RBAC model:
--   users → user_group_memberships → user_groups → group_roles → roles → role_permissions → permissions
--
-- All authentication and authorisation now uses the 'permissions' JWT claim.
-- Idempotent: IF EXISTS guard prevents failure on re-run.

ALTER TABLE users DROP COLUMN IF EXISTS role;
