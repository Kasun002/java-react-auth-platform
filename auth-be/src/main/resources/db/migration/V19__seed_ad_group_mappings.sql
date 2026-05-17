-- V19: Seed default AD group → local group mappings for local dev / CI.
-- These map the LDAP CNs defined in auth/docker/ldap/bootstrap.ldif to the
-- corresponding local UserGroups seeded in V11.
-- All inserts are idempotent via ON CONFLICT DO NOTHING.

INSERT INTO ad_group_mappings (ad_group_id, ad_group_name, local_group_id)
SELECT 'GRP-RETAIL-CUSTOMERS', 'Retail Banking Customers', g.id
FROM   user_groups g
WHERE  g.name = 'RETAIL_CUSTOMER'
ON CONFLICT (ad_group_id) DO NOTHING;

INSERT INTO ad_group_mappings (ad_group_id, ad_group_name, local_group_id)
SELECT 'GRP-BANK-STAFF', 'Bank Staff Members', g.id
FROM   user_groups g
WHERE  g.name = 'BANK_TELLER'
ON CONFLICT (ad_group_id) DO NOTHING;

INSERT INTO ad_group_mappings (ad_group_id, ad_group_name, local_group_id)
SELECT 'GRP-SYSTEM-ADMINS', 'System Administrators', g.id
FROM   user_groups g
WHERE  g.name = 'SYSTEM_ADMIN'
ON CONFLICT (ad_group_id) DO NOTHING;
