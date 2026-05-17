-- V21: Rename banking_roles table to roles.
--
-- PostgreSQL automatically updates all FK constraints that reference the
-- renamed table (role_permissions, group_roles, user_role_assignments).
-- Indexes on join tables remain valid — no index rebuild needed.
-- Idempotent: IF EXISTS guard prevents failure on re-run.

ALTER TABLE IF EXISTS banking_roles RENAME TO roles;

-- Rename the auto-created sequence so it matches the new table name.
ALTER SEQUENCE IF EXISTS banking_roles_id_seq RENAME TO roles_id_seq;
