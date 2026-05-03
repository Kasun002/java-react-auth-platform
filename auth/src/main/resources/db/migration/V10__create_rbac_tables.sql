-- V10: RBAC schema — permissions, banking_roles, user_groups and join tables
-- Zero impact on existing tables (V1–V9 untouched)

-- permissions: atomic operation codes (e.g. ACCOUNT_VIEW)
CREATE TABLE permissions (
    id          BIGSERIAL    PRIMARY KEY,
    code        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    category    VARCHAR(50)  NOT NULL,   -- USER | ACCOUNT | TRANSACTION | LOAN | COMPLIANCE | REPORT | ADMIN
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- banking_roles: named role definitions (e.g. ROLE_TELLER)
-- Named 'banking_roles' to avoid clash with reserved word 'roles' / java.sql.Role
CREATE TABLE banking_roles (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- role_permissions: which permissions belong to a role (M:M)
CREATE TABLE role_permissions (
    role_id       BIGINT NOT NULL REFERENCES banking_roles(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions(id)   ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- user_groups: group definitions (RETAIL_CUSTOMER, BANK_TELLER, etc.)
CREATE TABLE user_groups (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    type        VARCHAR(50)  NOT NULL,   -- CUSTOMER | STAFF | OVERSIGHT | ADMIN
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- group_roles: roles assigned to a group (M:M)
CREATE TABLE group_roles (
    group_id BIGINT NOT NULL REFERENCES user_groups(id)   ON DELETE CASCADE,
    role_id  BIGINT NOT NULL REFERENCES banking_roles(id) ON DELETE CASCADE,
    PRIMARY KEY (group_id, role_id)
);

-- user_group_memberships: which groups a user belongs to (M:M)
CREATE TABLE user_group_memberships (
    user_id     BIGINT    NOT NULL REFERENCES users(id)       ON DELETE CASCADE,
    group_id    BIGINT    NOT NULL REFERENCES user_groups(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, group_id)
);

-- user_role_assignments: direct role assignment bypassing groups (for special cases)
CREATE TABLE user_role_assignments (
    user_id     BIGINT    NOT NULL REFERENCES users(id)         ON DELETE CASCADE,
    role_id     BIGINT    NOT NULL REFERENCES banking_roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, role_id)
);

-- Performance indexes
CREATE INDEX idx_role_permissions_role  ON role_permissions(role_id);
CREATE INDEX idx_group_roles_group      ON group_roles(group_id);
CREATE INDEX idx_user_group_user        ON user_group_memberships(user_id);
CREATE INDEX idx_user_role_user         ON user_role_assignments(user_id);
