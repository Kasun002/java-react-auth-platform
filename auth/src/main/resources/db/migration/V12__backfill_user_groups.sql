-- V12: Backfill group memberships for users created before RBAC was introduced.
-- All existing USER-role accounts → RETAIL_CUSTOMER group.
-- All existing ADMIN-role accounts → SYSTEM_ADMIN group.
-- ON CONFLICT guard makes this idempotent (safe to run multiple times).

INSERT INTO user_group_memberships (user_id, group_id, assigned_at)
SELECT u.id, g.id, NOW()
FROM   users u
JOIN   user_groups g ON g.name = 'RETAIL_CUSTOMER'
WHERE  u.role = 'USER'
  AND  NOT EXISTS (
           SELECT 1
           FROM   user_group_memberships m
           WHERE  m.user_id = u.id
             AND  m.group_id = g.id
       );

INSERT INTO user_group_memberships (user_id, group_id, assigned_at)
SELECT u.id, g.id, NOW()
FROM   users u
JOIN   user_groups g ON g.name = 'SYSTEM_ADMIN'
WHERE  u.role = 'ADMIN'
  AND  NOT EXISTS (
           SELECT 1
           FROM   user_group_memberships m
           WHERE  m.user_id = u.id
             AND  m.group_id = g.id
       );
