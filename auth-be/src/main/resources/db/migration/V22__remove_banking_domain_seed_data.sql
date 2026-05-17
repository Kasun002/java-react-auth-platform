-- V22: Remove banking-domain seed data introduced in V11.
--
-- Keeps only platform-essential records:
--   Groups:      SYSTEM_ADMIN, SUPER_ADMIN
--   Roles:       ROLE_SYSTEM_ADMIN, ROLE_SUPER_ADMIN
--   Permissions: USER and ADMIN categories (the 12 platform-guarded permissions)
--
-- Deletes child join rows before parent rows to satisfy FK constraints.
-- All DELETEs are safe to re-run — idempotent by nature (rows already gone = no-op).

-- ── Step 1: Remove user memberships in banking groups ─────────────────────────
DELETE FROM user_group_memberships
WHERE group_id IN (
    SELECT id FROM user_groups
    WHERE name NOT IN ('SYSTEM_ADMIN', 'SUPER_ADMIN')
);

-- ── Step 2: Remove group→role mappings for banking groups ─────────────────────
DELETE FROM group_roles
WHERE group_id IN (
    SELECT id FROM user_groups
    WHERE name NOT IN ('SYSTEM_ADMIN', 'SUPER_ADMIN')
);

-- ── Step 3: Remove banking groups ─────────────────────────────────────────────
DELETE FROM user_groups
WHERE name NOT IN ('SYSTEM_ADMIN', 'SUPER_ADMIN');

-- ── Step 4: Remove direct user→role assignments for banking roles ──────────────
DELETE FROM user_role_assignments
WHERE role_id IN (
    SELECT id FROM roles
    WHERE name NOT IN ('ROLE_SYSTEM_ADMIN', 'ROLE_SUPER_ADMIN')
);

-- ── Step 5: Remove role→permission mappings for banking roles ─────────────────
DELETE FROM role_permissions
WHERE role_id IN (
    SELECT id FROM roles
    WHERE name NOT IN ('ROLE_SYSTEM_ADMIN', 'ROLE_SUPER_ADMIN')
);

-- ── Step 6: Remove banking roles ──────────────────────────────────────────────
DELETE FROM roles
WHERE name NOT IN ('ROLE_SYSTEM_ADMIN', 'ROLE_SUPER_ADMIN');

-- ── Step 7: Remove banking-domain permissions ─────────────────────────────────
-- Removes: ACCOUNT_*, TRANSACTION_*, LOAN_*, KYC_*, AML_*, REPORT_*
-- Keeps:   USER_* and ADMIN category permissions (platform-guarded endpoints)
DELETE FROM permissions
WHERE category IN ('ACCOUNT', 'TRANSACTION', 'LOAN', 'COMPLIANCE', 'REPORT');
