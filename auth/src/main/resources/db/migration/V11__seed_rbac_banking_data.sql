-- V11: Seed RBAC reference data — permissions, banking_roles, role_permissions, user_groups, group_roles
-- All inserts are idempotent via ON CONFLICT DO NOTHING

-- ── 1. Permissions ────────────────────────────────────────────────────────────

INSERT INTO permissions (code, description, category) VALUES
    -- USER
    ('USER_VIEW',            'Read user profile data',                          'USER'),
    ('USER_CREATE',          'Create new user accounts',                        'USER'),
    ('USER_UPDATE',          'Update user profile',                             'USER'),
    ('USER_DEACTIVATE',      'Deactivate or suspend accounts',                  'USER'),
    ('USER_GROUPS_MANAGE',   'Assign or remove users from groups',              'USER'),
    -- ACCOUNT
    ('ACCOUNT_VIEW',         'View account details and balance',                'ACCOUNT'),
    ('ACCOUNT_CREATE',       'Open new bank accounts',                          'ACCOUNT'),
    ('ACCOUNT_UPDATE',       'Modify account settings',                         'ACCOUNT'),
    ('ACCOUNT_CLOSE',        'Close or archive accounts',                       'ACCOUNT'),
    -- TRANSACTION
    ('TRANSACTION_VIEW',               'View transaction history',              'TRANSACTION'),
    ('TRANSACTION_INITIATE',           'Initiate transfers and payments',       'TRANSACTION'),
    ('TRANSACTION_APPROVE',            'Approve pending transactions',          'TRANSACTION'),
    ('TRANSACTION_REVERSE',            'Reverse or recall transactions',        'TRANSACTION'),
    ('TRANSACTION_HIGH_VALUE_APPROVE', 'Dual-control approval for high-value transactions (PCI-DSS Req 8.7)', 'TRANSACTION'),
    -- LOAN
    ('LOAN_VIEW',            'View loan applications and schedules',            'LOAN'),
    ('LOAN_APPLY',           'Submit a loan application',                       'LOAN'),
    ('LOAN_APPROVE',         'Approve or reject loan applications',             'LOAN'),
    ('LOAN_DISBURSE',        'Disburse approved loan funds',                    'LOAN'),
    -- COMPLIANCE
    ('KYC_VIEW',             'View KYC documents and status',                   'COMPLIANCE'),
    ('KYC_APPROVE',          'Approve KYC verification',                        'COMPLIANCE'),
    ('KYC_REJECT',           'Reject KYC with reason',                         'COMPLIANCE'),
    ('AML_FLAG_VIEW',        'View AML alerts and flags',                       'COMPLIANCE'),
    ('AML_FLAG_RESOLVE',     'Resolve or escalate AML flags',                  'COMPLIANCE'),
    -- REPORT
    ('REPORT_VIEW',          'View operational reports',                        'REPORT'),
    ('REPORT_EXPORT',        'Export reports to CSV or PDF',                    'REPORT'),
    ('REPORT_AUDIT_VIEW',    'View compliance and audit reports',               'REPORT'),
    -- ADMIN
    ('ROLE_MANAGE',          'Create, update, and delete roles',                'ADMIN'),
    ('GROUP_MANAGE',         'Create, update, and delete groups',               'ADMIN'),
    ('PERMISSION_MANAGE',    'Assign permissions to roles',                     'ADMIN'),
    ('SYSTEM_CONFIG_VIEW',   'View system configuration',                       'ADMIN'),
    ('SYSTEM_CONFIG_UPDATE', 'Modify system configuration',                     'ADMIN'),
    ('AUDIT_LOG_VIEW',       'View full audit trail',                           'ADMIN')
ON CONFLICT (code) DO NOTHING;


-- ── 2. Banking Roles ──────────────────────────────────────────────────────────

INSERT INTO banking_roles (name, description) VALUES
    ('ROLE_CUSTOMER_BASIC',   'Standard customer — view accounts, initiate transactions, apply for loans'),
    ('ROLE_CUSTOMER_PREMIUM', 'Premium customer — all basic permissions plus report access'),
    ('ROLE_TELLER',           'Front-line branch staff — account view and transaction initiation'),
    ('ROLE_LOAN_PROCESSOR',   'Loan origination and processing'),
    ('ROLE_LOAN_APPROVER',    'Loan approval authority including high-value transaction approval'),
    ('ROLE_RELATIONSHIP_MGR', 'Premium and corporate client relationship management'),
    ('ROLE_BRANCH_MANAGER',   'Branch-level approvals with user and report management'),
    ('ROLE_COMPLIANCE',       'AML/KYC monitoring and regulatory reporting'),
    ('ROLE_RISK',             'Credit and operational risk assessment'),
    ('ROLE_AUDITOR',          'Read-only audit access — no write permissions'),
    ('ROLE_SYSTEM_ADMIN',     'User and group management, system configuration'),
    ('ROLE_SUPER_ADMIN',      'Full access — admin of admins')
ON CONFLICT (name) DO NOTHING;


-- ── 3. Role → Permission Mappings ─────────────────────────────────────────────

-- ROLE_CUSTOMER_BASIC
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM banking_roles r, permissions p
WHERE r.name = 'ROLE_CUSTOMER_BASIC'
  AND p.code IN ('ACCOUNT_VIEW', 'TRANSACTION_VIEW', 'TRANSACTION_INITIATE', 'LOAN_VIEW', 'LOAN_APPLY')
ON CONFLICT DO NOTHING;

-- ROLE_CUSTOMER_PREMIUM (basic + REPORT_VIEW)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM banking_roles r, permissions p
WHERE r.name = 'ROLE_CUSTOMER_PREMIUM'
  AND p.code IN ('ACCOUNT_VIEW', 'TRANSACTION_VIEW', 'TRANSACTION_INITIATE', 'LOAN_VIEW', 'LOAN_APPLY', 'REPORT_VIEW')
ON CONFLICT DO NOTHING;

-- ROLE_TELLER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM banking_roles r, permissions p
WHERE r.name = 'ROLE_TELLER'
  AND p.code IN ('ACCOUNT_VIEW', 'TRANSACTION_VIEW', 'TRANSACTION_INITIATE', 'USER_VIEW')
ON CONFLICT DO NOTHING;

-- ROLE_LOAN_PROCESSOR
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM banking_roles r, permissions p
WHERE r.name = 'ROLE_LOAN_PROCESSOR'
  AND p.code IN ('LOAN_VIEW', 'LOAN_APPLY', 'LOAN_APPROVE', 'LOAN_DISBURSE', 'ACCOUNT_VIEW', 'USER_VIEW', 'KYC_VIEW')
ON CONFLICT DO NOTHING;

-- ROLE_LOAN_APPROVER (loan processor + transaction approvals)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM banking_roles r, permissions p
WHERE r.name = 'ROLE_LOAN_APPROVER'
  AND p.code IN ('LOAN_VIEW', 'LOAN_APPLY', 'LOAN_APPROVE', 'LOAN_DISBURSE',
                 'ACCOUNT_VIEW', 'USER_VIEW', 'KYC_VIEW',
                 'TRANSACTION_APPROVE', 'TRANSACTION_HIGH_VALUE_APPROVE')
ON CONFLICT DO NOTHING;

-- ROLE_RELATIONSHIP_MGR
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM banking_roles r, permissions p
WHERE r.name = 'ROLE_RELATIONSHIP_MGR'
  AND p.code IN ('ACCOUNT_VIEW', 'TRANSACTION_VIEW', 'USER_VIEW', 'USER_UPDATE', 'LOAN_VIEW', 'REPORT_VIEW')
ON CONFLICT DO NOTHING;

-- ROLE_BRANCH_MANAGER (loan approver + user mgmt + reports)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM banking_roles r, permissions p
WHERE r.name = 'ROLE_BRANCH_MANAGER'
  AND p.code IN ('LOAN_VIEW', 'LOAN_APPLY', 'LOAN_APPROVE', 'LOAN_DISBURSE',
                 'ACCOUNT_VIEW', 'USER_VIEW', 'KYC_VIEW',
                 'TRANSACTION_APPROVE', 'TRANSACTION_HIGH_VALUE_APPROVE',
                 'USER_CREATE', 'USER_UPDATE',
                 'REPORT_VIEW', 'REPORT_EXPORT')
ON CONFLICT DO NOTHING;

-- ROLE_COMPLIANCE
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM banking_roles r, permissions p
WHERE r.name = 'ROLE_COMPLIANCE'
  AND p.code IN ('KYC_VIEW', 'KYC_APPROVE', 'KYC_REJECT',
                 'AML_FLAG_VIEW', 'AML_FLAG_RESOLVE',
                 'ACCOUNT_VIEW', 'TRANSACTION_VIEW',
                 'REPORT_VIEW', 'REPORT_AUDIT_VIEW', 'AUDIT_LOG_VIEW')
ON CONFLICT DO NOTHING;

-- ROLE_RISK
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM banking_roles r, permissions p
WHERE r.name = 'ROLE_RISK'
  AND p.code IN ('ACCOUNT_VIEW', 'TRANSACTION_VIEW', 'LOAN_VIEW',
                 'REPORT_VIEW', 'REPORT_AUDIT_VIEW', 'AML_FLAG_VIEW')
ON CONFLICT DO NOTHING;

-- ROLE_AUDITOR (read-only)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM banking_roles r, permissions p
WHERE r.name = 'ROLE_AUDITOR'
  AND p.code IN ('AUDIT_LOG_VIEW', 'REPORT_VIEW', 'REPORT_AUDIT_VIEW',
                 'ACCOUNT_VIEW', 'TRANSACTION_VIEW', 'USER_VIEW')
ON CONFLICT DO NOTHING;

-- ROLE_SYSTEM_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM banking_roles r, permissions p
WHERE r.name = 'ROLE_SYSTEM_ADMIN'
  AND p.code IN ('USER_VIEW', 'USER_CREATE', 'USER_UPDATE', 'USER_DEACTIVATE', 'USER_GROUPS_MANAGE',
                 'ROLE_MANAGE', 'GROUP_MANAGE',
                 'AUDIT_LOG_VIEW', 'SYSTEM_CONFIG_VIEW', 'SYSTEM_CONFIG_UPDATE')
ON CONFLICT DO NOTHING;

-- ROLE_SUPER_ADMIN (all permissions)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM banking_roles r, permissions p
WHERE r.name = 'ROLE_SUPER_ADMIN'
ON CONFLICT DO NOTHING;


-- ── 4. User Groups ────────────────────────────────────────────────────────────

INSERT INTO user_groups (name, description, type) VALUES
    -- Customer groups
    ('RETAIL_CUSTOMER',    'Standard individual account holder',                     'CUSTOMER'),
    ('PREMIUM_CUSTOMER',   'Priority/HNI banking customer',                          'CUSTOMER'),
    ('CORPORATE_CUSTOMER', 'Business or SME account holder',                         'CUSTOMER'),
    -- Staff groups
    ('BANK_TELLER',        'Front-line branch, cash transactions',                   'STAFF'),
    ('LOAN_OFFICER',       'Loan origination and processing',                        'STAFF'),
    ('RELATIONSHIP_MANAGER', 'Premium and corporate client management',              'STAFF'),
    ('BRANCH_MANAGER',     'Branch-level approvals, escalated authority',            'STAFF'),
    ('OPERATIONS_STAFF',   'Back-office processing',                                 'STAFF'),
    -- Oversight groups
    ('COMPLIANCE_OFFICER', 'AML/KYC monitoring, regulatory reporting',               'OVERSIGHT'),
    ('RISK_ANALYST',       'Credit and operational risk assessment',                  'OVERSIGHT'),
    ('INTERNAL_AUDITOR',   'Read-only audit access, no write permissions',           'OVERSIGHT'),
    -- Admin groups
    ('SYSTEM_ADMIN',       'User and group management, system configuration',        'ADMIN'),
    ('SUPER_ADMIN',        'Full access, admin of admins',                           'ADMIN')
ON CONFLICT (name) DO NOTHING;


-- ── 5. Group → Role Mappings ──────────────────────────────────────────────────

INSERT INTO group_roles (group_id, role_id)
SELECT g.id, r.id FROM user_groups g, banking_roles r
WHERE g.name = 'RETAIL_CUSTOMER'    AND r.name = 'ROLE_CUSTOMER_BASIC'
ON CONFLICT DO NOTHING;

INSERT INTO group_roles (group_id, role_id)
SELECT g.id, r.id FROM user_groups g, banking_roles r
WHERE g.name = 'PREMIUM_CUSTOMER'   AND r.name IN ('ROLE_CUSTOMER_BASIC', 'ROLE_CUSTOMER_PREMIUM')
ON CONFLICT DO NOTHING;

INSERT INTO group_roles (group_id, role_id)
SELECT g.id, r.id FROM user_groups g, banking_roles r
WHERE g.name = 'CORPORATE_CUSTOMER' AND r.name = 'ROLE_CUSTOMER_BASIC'
ON CONFLICT DO NOTHING;

INSERT INTO group_roles (group_id, role_id)
SELECT g.id, r.id FROM user_groups g, banking_roles r
WHERE g.name = 'BANK_TELLER'        AND r.name = 'ROLE_TELLER'
ON CONFLICT DO NOTHING;

INSERT INTO group_roles (group_id, role_id)
SELECT g.id, r.id FROM user_groups g, banking_roles r
WHERE g.name = 'LOAN_OFFICER'       AND r.name IN ('ROLE_LOAN_PROCESSOR', 'ROLE_TELLER')
ON CONFLICT DO NOTHING;

INSERT INTO group_roles (group_id, role_id)
SELECT g.id, r.id FROM user_groups g, banking_roles r
WHERE g.name = 'RELATIONSHIP_MANAGER' AND r.name = 'ROLE_RELATIONSHIP_MGR'
ON CONFLICT DO NOTHING;

INSERT INTO group_roles (group_id, role_id)
SELECT g.id, r.id FROM user_groups g, banking_roles r
WHERE g.name = 'BRANCH_MANAGER'     AND r.name IN ('ROLE_BRANCH_MANAGER', 'ROLE_LOAN_APPROVER')
ON CONFLICT DO NOTHING;

INSERT INTO group_roles (group_id, role_id)
SELECT g.id, r.id FROM user_groups g, banking_roles r
WHERE g.name = 'OPERATIONS_STAFF'   AND r.name = 'ROLE_TELLER'
ON CONFLICT DO NOTHING;

INSERT INTO group_roles (group_id, role_id)
SELECT g.id, r.id FROM user_groups g, banking_roles r
WHERE g.name = 'COMPLIANCE_OFFICER' AND r.name = 'ROLE_COMPLIANCE'
ON CONFLICT DO NOTHING;

INSERT INTO group_roles (group_id, role_id)
SELECT g.id, r.id FROM user_groups g, banking_roles r
WHERE g.name = 'RISK_ANALYST'       AND r.name = 'ROLE_RISK'
ON CONFLICT DO NOTHING;

INSERT INTO group_roles (group_id, role_id)
SELECT g.id, r.id FROM user_groups g, banking_roles r
WHERE g.name = 'INTERNAL_AUDITOR'   AND r.name = 'ROLE_AUDITOR'
ON CONFLICT DO NOTHING;

INSERT INTO group_roles (group_id, role_id)
SELECT g.id, r.id FROM user_groups g, banking_roles r
WHERE g.name = 'SYSTEM_ADMIN'       AND r.name = 'ROLE_SYSTEM_ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO group_roles (group_id, role_id)
SELECT g.id, r.id FROM user_groups g, banking_roles r
WHERE g.name = 'SUPER_ADMIN'        AND r.name = 'ROLE_SUPER_ADMIN'
ON CONFLICT DO NOTHING;
