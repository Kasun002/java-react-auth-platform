// Temporary RBAC sample data — replace with API calls after backend integration

export interface Permission {
  id: string;
  name: string;
  resource: string;
  action: string;
  description: string;
  category: string;
  riskLevel: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
  createdAt: string;
}

export interface Role {
  id: string;
  name: string;
  description: string;
  permissions: string[];
  userCount: number;
  isSystemRole: boolean;
  createdAt: string;
}

export interface Group {
  id: string;
  name: string;
  description: string;
  roles: string[];
  members: string[];
  department: string;
  createdAt: string;
}

export interface User {
  id: string;
  name: string;
  email: string;
  status: "ACTIVE" | "INACTIVE" | "SUSPENDED";
  authProvider: "LOCAL" | "AZURE_AD" | "LDAP";
  groups: string[];
  roles: string[];
  lastLoginAt: string;
  createdAt: string;
  department: string;
}

export interface AuditEntry {
  id: string;
  userId: string;
  userName: string;
  action: string;
  resource: string;
  resourceId: string;
  details: string;
  ipAddress: string;
  status: "SUCCESS" | "FAILURE" | "WARNING";
  timestamp: string;
}

export const PERMISSIONS: Permission[] = [
  { id: "perm-001", name: "user:read", resource: "USER", action: "READ", description: "View user profiles and basic information", category: "User Management", riskLevel: "LOW", createdAt: "2024-01-15T08:00:00Z" },
  { id: "perm-002", name: "user:create", resource: "USER", action: "CREATE", description: "Create new user accounts", category: "User Management", riskLevel: "MEDIUM", createdAt: "2024-01-15T08:00:00Z" },
  { id: "perm-003", name: "user:update", resource: "USER", action: "UPDATE", description: "Modify existing user account details", category: "User Management", riskLevel: "MEDIUM", createdAt: "2024-01-15T08:00:00Z" },
  { id: "perm-004", name: "user:delete", resource: "USER", action: "DELETE", description: "Permanently remove user accounts", category: "User Management", riskLevel: "HIGH", createdAt: "2024-01-15T08:00:00Z" },
  { id: "perm-005", name: "user:suspend", resource: "USER", action: "SUSPEND", description: "Temporarily suspend user access", category: "User Management", riskLevel: "HIGH", createdAt: "2024-01-15T08:00:00Z" },
  { id: "perm-006", name: "role:read", resource: "ROLE", action: "READ", description: "View roles and their permissions", category: "Role Management", riskLevel: "LOW", createdAt: "2024-01-15T08:00:00Z" },
  { id: "perm-007", name: "role:create", resource: "ROLE", action: "CREATE", description: "Create new roles", category: "Role Management", riskLevel: "HIGH", createdAt: "2024-01-15T08:00:00Z" },
  { id: "perm-008", name: "role:update", resource: "ROLE", action: "UPDATE", description: "Modify role permissions", category: "Role Management", riskLevel: "CRITICAL", createdAt: "2024-01-15T08:00:00Z" },
  { id: "perm-009", name: "role:delete", resource: "ROLE", action: "DELETE", description: "Remove roles from the system", category: "Role Management", riskLevel: "CRITICAL", createdAt: "2024-01-15T08:00:00Z" },
  { id: "perm-010", name: "role:assign", resource: "ROLE", action: "ASSIGN", description: "Assign roles to users or groups", category: "Role Management", riskLevel: "HIGH", createdAt: "2024-01-15T08:00:00Z" },
  { id: "perm-011", name: "group:read", resource: "GROUP", action: "READ", description: "View groups and their members", category: "Group Management", riskLevel: "LOW", createdAt: "2024-01-15T08:00:00Z" },
  { id: "perm-012", name: "group:create", resource: "GROUP", action: "CREATE", description: "Create new groups", category: "Group Management", riskLevel: "MEDIUM", createdAt: "2024-01-15T08:00:00Z" },
  { id: "perm-013", name: "group:update", resource: "GROUP", action: "UPDATE", description: "Modify group membership and roles", category: "Group Management", riskLevel: "HIGH", createdAt: "2024-01-15T08:00:00Z" },
  { id: "perm-014", name: "group:delete", resource: "GROUP", action: "DELETE", description: "Remove groups from the system", category: "Group Management", riskLevel: "HIGH", createdAt: "2024-01-15T08:00:00Z" },
  { id: "perm-015", name: "permission:read", resource: "PERMISSION", action: "READ", description: "View system permissions", category: "Permission Management", riskLevel: "LOW", createdAt: "2024-01-15T08:00:00Z" },
  { id: "perm-016", name: "permission:create", resource: "PERMISSION", action: "CREATE", description: "Create new permissions", category: "Permission Management", riskLevel: "CRITICAL", createdAt: "2024-01-15T08:00:00Z" },
  { id: "perm-017", name: "permission:update", resource: "PERMISSION", action: "UPDATE", description: "Modify existing permissions", category: "Permission Management", riskLevel: "CRITICAL", createdAt: "2024-01-15T08:00:00Z" },
  { id: "perm-018", name: "permission:delete", resource: "PERMISSION", action: "DELETE", description: "Remove permissions from the system", category: "Permission Management", riskLevel: "CRITICAL", createdAt: "2024-01-15T08:00:00Z" },
  { id: "perm-019", name: "audit:read", resource: "AUDIT", action: "READ", description: "View audit logs and security events", category: "Audit & Compliance", riskLevel: "MEDIUM", createdAt: "2024-01-15T08:00:00Z" },
  { id: "perm-020", name: "audit:export", resource: "AUDIT", action: "EXPORT", description: "Export audit logs for compliance", category: "Audit & Compliance", riskLevel: "HIGH", createdAt: "2024-01-15T08:00:00Z" },
  { id: "perm-021", name: "account:read", resource: "ACCOUNT", action: "READ", description: "View account information", category: "Account Operations", riskLevel: "LOW", createdAt: "2024-01-15T08:00:00Z" },
  { id: "perm-022", name: "account:transfer", resource: "ACCOUNT", action: "TRANSFER", description: "Initiate fund transfers", category: "Account Operations", riskLevel: "CRITICAL", createdAt: "2024-01-15T08:00:00Z" },
  { id: "perm-023", name: "account:freeze", resource: "ACCOUNT", action: "FREEZE", description: "Freeze account operations", category: "Account Operations", riskLevel: "HIGH", createdAt: "2024-01-15T08:00:00Z" },
  { id: "perm-024", name: "report:read", resource: "REPORT", action: "READ", description: "View reports and analytics", category: "Reporting", riskLevel: "LOW", createdAt: "2024-01-15T08:00:00Z" },
  { id: "perm-025", name: "report:create", resource: "REPORT", action: "CREATE", description: "Generate custom reports", category: "Reporting", riskLevel: "MEDIUM", createdAt: "2024-01-15T08:00:00Z" },
  { id: "perm-026", name: "system:config", resource: "SYSTEM", action: "CONFIG", description: "Modify system configuration", category: "System Administration", riskLevel: "CRITICAL", createdAt: "2024-01-15T08:00:00Z" },
  { id: "perm-027", name: "system:backup", resource: "SYSTEM", action: "BACKUP", description: "Perform system backups", category: "System Administration", riskLevel: "HIGH", createdAt: "2024-01-15T08:00:00Z" },
  { id: "perm-028", name: "customer:read", resource: "CUSTOMER", action: "READ", description: "View customer profiles and KYC data", category: "Customer Service", riskLevel: "MEDIUM", createdAt: "2024-01-15T08:00:00Z" },
  { id: "perm-029", name: "customer:update", resource: "CUSTOMER", action: "UPDATE", description: "Update customer information", category: "Customer Service", riskLevel: "HIGH", createdAt: "2024-01-15T08:00:00Z" },
  { id: "perm-030", name: "loan:approve", resource: "LOAN", action: "APPROVE", description: "Approve loan applications", category: "Loan Management", riskLevel: "CRITICAL", createdAt: "2024-01-15T08:00:00Z" },
  { id: "perm-031", name: "loan:read", resource: "LOAN", action: "READ", description: "View loan applications and status", category: "Loan Management", riskLevel: "LOW", createdAt: "2024-01-15T08:00:00Z" },
  { id: "perm-032", name: "compliance:review", resource: "COMPLIANCE", action: "REVIEW", description: "Review compliance flags and AML alerts", category: "Compliance", riskLevel: "CRITICAL", createdAt: "2024-01-15T08:00:00Z" },
];

export const ROLES: Role[] = [
  { id: "role-001", name: "SUPER_ADMIN", description: "Full system access — all permissions including system configuration", permissions: PERMISSIONS.map(p => p.id), userCount: 2, isSystemRole: true, createdAt: "2024-01-15T08:00:00Z" },
  { id: "role-002", name: "ADMIN", description: "Administrative access — user/role/group management, audit access", permissions: ["perm-001","perm-002","perm-003","perm-004","perm-005","perm-006","perm-007","perm-008","perm-009","perm-010","perm-011","perm-012","perm-013","perm-014","perm-015","perm-019","perm-020","perm-024","perm-025"], userCount: 5, isSystemRole: true, createdAt: "2024-01-15T08:00:00Z" },
  { id: "role-003", name: "SECURITY_OFFICER", description: "Security monitoring — audit logs, user suspension, compliance review", permissions: ["perm-001","perm-005","perm-006","perm-011","perm-015","perm-019","perm-020","perm-032"], userCount: 3, isSystemRole: false, createdAt: "2024-01-20T09:00:00Z" },
  { id: "role-004", name: "BRANCH_MANAGER", description: "Branch operations — account management, loan approvals, customer service", permissions: ["perm-001","perm-021","perm-022","perm-023","perm-024","perm-028","perm-029","perm-030","perm-031"], userCount: 8, isSystemRole: false, createdAt: "2024-01-20T09:00:00Z" },
  { id: "role-005", name: "TELLER", description: "Front-line banking — view accounts, initiate transfers (with approval)", permissions: ["perm-001","perm-021","perm-024","perm-028","perm-031"], userCount: 24, isSystemRole: false, createdAt: "2024-01-20T09:00:00Z" },
  { id: "role-006", name: "LOAN_OFFICER", description: "Loan processing — review applications, approve loans, customer updates", permissions: ["perm-001","perm-021","perm-028","perm-029","perm-030","perm-031"], userCount: 12, isSystemRole: false, createdAt: "2024-01-25T10:00:00Z" },
  { id: "role-007", name: "COMPLIANCE_ANALYST", description: "Regulatory compliance — audit review, compliance flagging, reporting", permissions: ["perm-001","perm-019","perm-020","perm-024","perm-025","perm-028","perm-032"], userCount: 6, isSystemRole: false, createdAt: "2024-01-25T10:00:00Z" },
  { id: "role-008", name: "CUSTOMER_SERVICE", description: "Customer support — view customer profiles, update non-sensitive info", permissions: ["perm-001","perm-021","perm-024","perm-028"], userCount: 18, isSystemRole: false, createdAt: "2024-02-01T08:00:00Z" },
  { id: "role-009", name: "AUDITOR", description: "Internal audit — read-only access to all audit logs and reports", permissions: ["perm-001","perm-006","perm-011","perm-015","perm-019","perm-020","perm-024","perm-025"], userCount: 4, isSystemRole: false, createdAt: "2024-02-01T08:00:00Z" },
  { id: "role-010", name: "IT_SUPPORT", description: "Technical support — user account management, system backup", permissions: ["perm-001","perm-002","perm-003","perm-006","perm-011","perm-019","perm-027"], userCount: 7, isSystemRole: false, createdAt: "2024-02-05T08:00:00Z" },
  { id: "role-011", name: "REPORT_VIEWER", description: "Reporting only — view and export reports, no modification rights", permissions: ["perm-001","perm-024","perm-025"], userCount: 15, isSystemRole: false, createdAt: "2024-02-10T08:00:00Z" },
  { id: "role-012", name: "READ_ONLY", description: "Minimal access — view-only across all resources for contractors/auditors", permissions: ["perm-001","perm-006","perm-011","perm-015","perm-019","perm-021","perm-024","perm-028","perm-031"], userCount: 9, isSystemRole: false, createdAt: "2024-02-10T08:00:00Z" },
];

export const GROUPS: Group[] = [
  { id: "grp-001", name: "Executive Leadership", description: "C-suite and senior management with full system access", roles: ["role-001", "role-002"], members: ["usr-001", "usr-002"], department: "Executive", createdAt: "2024-01-15T08:00:00Z" },
  { id: "grp-002", name: "IT Administration", description: "System administrators and IT operations team", roles: ["role-002", "role-010"], members: ["usr-003", "usr-004", "usr-005"], department: "IT", createdAt: "2024-01-15T08:00:00Z" },
  { id: "grp-003", name: "Security Team", description: "Information security and fraud prevention", roles: ["role-003", "role-009"], members: ["usr-006", "usr-007", "usr-008"], department: "Security", createdAt: "2024-01-20T09:00:00Z" },
  { id: "grp-004", name: "Colombo Branch", description: "Colombo main branch operations staff", roles: ["role-004", "role-005"], members: ["usr-009", "usr-010", "usr-011", "usr-012"], department: "Retail Banking", createdAt: "2024-01-20T09:00:00Z" },
  { id: "grp-005", name: "Kandy Branch", description: "Kandy branch operations staff", roles: ["role-004", "role-005"], members: ["usr-013", "usr-014"], department: "Retail Banking", createdAt: "2024-01-25T10:00:00Z" },
  { id: "grp-006", name: "Galle Branch", description: "Galle branch operations staff", roles: ["role-005", "role-008"], members: ["usr-015", "usr-016"], department: "Retail Banking", createdAt: "2024-01-25T10:00:00Z" },
  { id: "grp-007", name: "Loan Department", description: "Loan processing and approval team", roles: ["role-006"], members: ["usr-017", "usr-018", "usr-019"], department: "Credit", createdAt: "2024-02-01T08:00:00Z" },
  { id: "grp-008", name: "Compliance & Risk", description: "Regulatory compliance and risk management", roles: ["role-007", "role-009"], members: ["usr-020", "usr-021"], department: "Compliance", createdAt: "2024-02-01T08:00:00Z" },
  { id: "grp-009", name: "Customer Service Centre", description: "Centralised customer support team", roles: ["role-008"], members: ["usr-022", "usr-010"], department: "Customer Service", createdAt: "2024-02-05T08:00:00Z" },
  { id: "grp-010", name: "Internal Audit", description: "Independent internal audit function", roles: ["role-009"], members: ["usr-020"], department: "Audit", createdAt: "2024-02-05T08:00:00Z" },
  { id: "grp-011", name: "Finance Reporting", description: "Financial reporting and analytics team", roles: ["role-011"], members: ["usr-003", "usr-021"], department: "Finance", createdAt: "2024-02-10T08:00:00Z" },
  { id: "grp-012", name: "External Auditors", description: "External audit firm with read-only access", roles: ["role-012"], members: [], department: "External", createdAt: "2024-02-10T08:00:00Z" },
  { id: "grp-013", name: "Contractors", description: "Temporary contractor accounts with minimal access", roles: ["role-012"], members: [], department: "External", createdAt: "2024-03-01T08:00:00Z" },
];

export const USERS: User[] = [
  { id: "usr-001", name: "Priya Wickramasinghe", email: "priya.w@corp.example.com", status: "ACTIVE", authProvider: "AZURE_AD", groups: ["grp-001"], roles: ["role-001"], lastLoginAt: "2025-05-07T14:32:00Z", createdAt: "2024-01-15T08:00:00Z", department: "Executive" },
  { id: "usr-002", name: "Rohan Fernando", email: "rohan.f@corp.example.com", status: "ACTIVE", authProvider: "AZURE_AD", groups: ["grp-001"], roles: ["role-001", "role-002"], lastLoginAt: "2025-05-06T09:15:00Z", createdAt: "2024-01-15T08:00:00Z", department: "Executive" },
  { id: "usr-003", name: "Chamara Perera", email: "chamara.p@corp.example.com", status: "ACTIVE", authProvider: "LDAP", groups: ["grp-002", "grp-011"], roles: ["role-002", "role-011"], lastLoginAt: "2025-05-07T08:45:00Z", createdAt: "2024-01-15T08:00:00Z", department: "IT" },
  { id: "usr-004", name: "Nishantha Jayawardena", email: "nishantha.j@corp.example.com", status: "ACTIVE", authProvider: "LDAP", groups: ["grp-002"], roles: ["role-010"], lastLoginAt: "2025-05-07T11:20:00Z", createdAt: "2024-01-15T08:00:00Z", department: "IT" },
  { id: "usr-005", name: "Dilini Samarawickrama", email: "dilini.s@corp.example.com", status: "ACTIVE", authProvider: "LOCAL", groups: ["grp-002"], roles: ["role-010"], lastLoginAt: "2025-05-05T16:30:00Z", createdAt: "2024-02-01T08:00:00Z", department: "IT" },
  { id: "usr-006", name: "Kasun Rajapaksa", email: "kasun.r@corp.example.com", status: "ACTIVE", authProvider: "AZURE_AD", groups: ["grp-003"], roles: ["role-003"], lastLoginAt: "2025-05-07T07:00:00Z", createdAt: "2024-01-20T09:00:00Z", department: "Security" },
  { id: "usr-007", name: "Thilini Bandara", email: "thilini.b@corp.example.com", status: "ACTIVE", authProvider: "AZURE_AD", groups: ["grp-003"], roles: ["role-003", "role-009"], lastLoginAt: "2025-05-06T17:45:00Z", createdAt: "2024-01-20T09:00:00Z", department: "Security" },
  { id: "usr-008", name: "Supun Karunarathne", email: "supun.k@corp.example.com", status: "INACTIVE", authProvider: "LOCAL", groups: ["grp-003"], roles: ["role-003"], lastLoginAt: "2025-04-10T12:00:00Z", createdAt: "2024-01-20T09:00:00Z", department: "Security" },
  { id: "usr-009", name: "Amali Silva", email: "amali.s@corp.example.com", status: "ACTIVE", authProvider: "LOCAL", groups: ["grp-004"], roles: ["role-004"], lastLoginAt: "2025-05-07T09:30:00Z", createdAt: "2024-01-20T09:00:00Z", department: "Retail Banking" },
  { id: "usr-010", name: "Buddhika Mendis", email: "buddhika.m@corp.example.com", status: "ACTIVE", authProvider: "LOCAL", groups: ["grp-004", "grp-009"], roles: ["role-005", "role-008"], lastLoginAt: "2025-05-07T13:00:00Z", createdAt: "2024-01-25T10:00:00Z", department: "Retail Banking" },
  { id: "usr-011", name: "Nadeesha Gunawardena", email: "nadeesha.g@corp.example.com", status: "ACTIVE", authProvider: "LOCAL", groups: ["grp-004"], roles: ["role-005"], lastLoginAt: "2025-05-07T10:15:00Z", createdAt: "2024-01-25T10:00:00Z", department: "Retail Banking" },
  { id: "usr-012", name: "Tharaka Wijesekara", email: "tharaka.w@corp.example.com", status: "SUSPENDED", authProvider: "LOCAL", groups: ["grp-004"], roles: ["role-005"], lastLoginAt: "2025-04-28T08:00:00Z", createdAt: "2024-02-01T08:00:00Z", department: "Retail Banking" },
  { id: "usr-013", name: "Sanduni Herath", email: "sanduni.h@corp.example.com", status: "ACTIVE", authProvider: "LOCAL", groups: ["grp-005"], roles: ["role-004"], lastLoginAt: "2025-05-07T08:00:00Z", createdAt: "2024-01-25T10:00:00Z", department: "Retail Banking" },
  { id: "usr-014", name: "Malith Dissanayake", email: "malith.d@corp.example.com", status: "ACTIVE", authProvider: "LOCAL", groups: ["grp-005"], roles: ["role-005"], lastLoginAt: "2025-05-06T15:30:00Z", createdAt: "2024-02-01T08:00:00Z", department: "Retail Banking" },
  { id: "usr-015", name: "Chathuri Weerasinghe", email: "chathuri.w@corp.example.com", status: "ACTIVE", authProvider: "LOCAL", groups: ["grp-006"], roles: ["role-005"], lastLoginAt: "2025-05-07T11:00:00Z", createdAt: "2024-02-01T08:00:00Z", department: "Retail Banking" },
  { id: "usr-016", name: "Dinesh Jayasuriya", email: "dinesh.j@corp.example.com", status: "ACTIVE", authProvider: "LOCAL", groups: ["grp-006"], roles: ["role-008"], lastLoginAt: "2025-05-05T14:45:00Z", createdAt: "2024-02-05T08:00:00Z", department: "Retail Banking" },
  { id: "usr-017", name: "Hasini Ranatunga", email: "hasini.r@corp.example.com", status: "ACTIVE", authProvider: "AZURE_AD", groups: ["grp-007"], roles: ["role-006"], lastLoginAt: "2025-05-07T09:00:00Z", createdAt: "2024-02-01T08:00:00Z", department: "Credit" },
  { id: "usr-018", name: "Ruwantha Seneviratne", email: "ruwantha.s@corp.example.com", status: "ACTIVE", authProvider: "AZURE_AD", groups: ["grp-007"], roles: ["role-006"], lastLoginAt: "2025-05-07T10:30:00Z", createdAt: "2024-02-05T08:00:00Z", department: "Credit" },
  { id: "usr-019", name: "Yohan Appuhamy", email: "yohan.a@corp.example.com", status: "INACTIVE", authProvider: "LOCAL", groups: ["grp-007"], roles: ["role-006"], lastLoginAt: "2025-03-20T08:00:00Z", createdAt: "2024-02-05T08:00:00Z", department: "Credit" },
  { id: "usr-020", name: "Sachini Liyanage", email: "sachini.l@corp.example.com", status: "ACTIVE", authProvider: "AZURE_AD", groups: ["grp-008", "grp-010"], roles: ["role-007", "role-009"], lastLoginAt: "2025-05-07T08:30:00Z", createdAt: "2024-02-01T08:00:00Z", department: "Compliance" },
  { id: "usr-021", name: "Kanishka Pathirana", email: "kanishka.p@corp.example.com", status: "ACTIVE", authProvider: "AZURE_AD", groups: ["grp-008", "grp-011"], roles: ["role-007", "role-011"], lastLoginAt: "2025-05-06T16:00:00Z", createdAt: "2024-02-05T08:00:00Z", department: "Compliance" },
  { id: "usr-022", name: "Dulani Wickremaratne", email: "dulani.w@corp.example.com", status: "ACTIVE", authProvider: "LOCAL", groups: ["grp-009"], roles: ["role-008"], lastLoginAt: "2025-05-07T12:15:00Z", createdAt: "2024-02-10T08:00:00Z", department: "Customer Service" },
];

export const AUDIT: AuditEntry[] = [
  { id: "aud-001", userId: "usr-003", userName: "Chamara Perera", action: "ROLE_ASSIGNED", resource: "USER", resourceId: "usr-015", details: "Assigned role TELLER to Chathuri Weerasinghe", ipAddress: "192.168.1.45", status: "SUCCESS", timestamp: "2025-05-07T14:32:11Z" },
  { id: "aud-002", userId: "usr-006", userName: "Kasun Rajapaksa", action: "USER_SUSPENDED", resource: "USER", resourceId: "usr-012", details: "Suspended Tharaka Wijesekara — policy violation investigation", ipAddress: "10.0.0.12", status: "SUCCESS", timestamp: "2025-05-07T13:15:44Z" },
  { id: "aud-003", userId: "usr-001", userName: "Priya Wickramasinghe", action: "ROLE_CREATED", resource: "ROLE", resourceId: "role-012", details: "Created READ_ONLY role for external auditor access", ipAddress: "10.0.0.5", status: "SUCCESS", timestamp: "2025-05-07T11:00:22Z" },
  { id: "aud-004", userId: "usr-012", userName: "Tharaka Wijesekara", action: "LOGIN_FAILED", resource: "AUTH", resourceId: "usr-012", details: "Multiple failed login attempts detected — account flagged", ipAddress: "203.94.12.77", status: "FAILURE", timestamp: "2025-05-07T10:45:08Z" },
  { id: "aud-005", userId: "usr-020", userName: "Sachini Liyanage", action: "AUDIT_EXPORTED", resource: "AUDIT", resourceId: "export-2025-05", details: "Exported audit logs for May 2025 compliance report", ipAddress: "192.168.2.30", status: "SUCCESS", timestamp: "2025-05-07T09:30:55Z" },
  { id: "aud-006", userId: "usr-004", userName: "Nishantha Jayawardena", action: "PERMISSION_UPDATED", resource: "ROLE", resourceId: "role-005", details: "Removed account:transfer from TELLER role — requires dual approval", ipAddress: "10.0.0.22", status: "SUCCESS", timestamp: "2025-05-06T17:22:33Z" },
  { id: "aud-007", userId: "usr-007", userName: "Thilini Bandara", action: "GROUP_MEMBER_REMOVED", resource: "GROUP", resourceId: "grp-003", details: "Removed Supun Karunarathne from Security Team", ipAddress: "10.0.0.11", status: "SUCCESS", timestamp: "2025-05-06T15:10:17Z" },
  { id: "aud-008", userId: "usr-002", userName: "Rohan Fernando", action: "SYSTEM_CONFIG_CHANGED", resource: "SYSTEM", resourceId: "config-jwt", details: "Updated JWT token expiry from 30m to 15m — security hardening", ipAddress: "10.0.0.3", status: "SUCCESS", timestamp: "2025-05-06T14:00:00Z" },
  { id: "aud-009", userId: "usr-017", userName: "Hasini Ranatunga", action: "LOAN_APPROVED", resource: "LOAN", resourceId: "loan-4821", details: "Approved personal loan application LN-4821 — LKR 2.5M", ipAddress: "192.168.3.15", status: "SUCCESS", timestamp: "2025-05-06T11:45:29Z" },
  { id: "aud-010", userId: "usr-022", userName: "Dulani Wickremaratne", action: "CUSTOMER_UPDATED", resource: "CUSTOMER", resourceId: "cust-9934", details: "Updated contact information for customer C-9934", ipAddress: "192.168.1.88", status: "SUCCESS", timestamp: "2025-05-06T10:30:42Z" },
  { id: "aud-011", userId: "usr-008", userName: "Supun Karunarathne", action: "UNAUTHORIZED_ACCESS", resource: "SYSTEM", resourceId: "config-system", details: "Attempted to access system configuration without permission", ipAddress: "192.168.1.99", status: "FAILURE", timestamp: "2025-05-05T16:55:00Z" },
  { id: "aud-012", userId: "usr-003", userName: "Chamara Perera", action: "USER_CREATED", resource: "USER", resourceId: "usr-022", details: "Created new user account for Dulani Wickremaratne (Customer Service)", ipAddress: "10.0.0.45", status: "SUCCESS", timestamp: "2025-05-05T09:00:00Z" },
  { id: "aud-013", userId: "usr-021", userName: "Kanishka Pathirana", action: "COMPLIANCE_FLAGGED", resource: "COMPLIANCE", resourceId: "txn-88231", details: "Flagged transaction TXN-88231 for AML review — unusual pattern", ipAddress: "192.168.2.31", status: "WARNING", timestamp: "2025-05-04T14:22:18Z" },
];

// ── Helper functions ──────────────────────────────────────────────────────────

export function getPermissionsByCategory(): Record<string, Permission[]> {
  return PERMISSIONS.reduce<Record<string, Permission[]>>((acc, p) => {
    if (!acc[p.category]) acc[p.category] = [];
    acc[p.category].push(p);
    return acc;
  }, {});
}

export function getUsersByStatus(): Record<string, number> {
  return USERS.reduce<Record<string, number>>((acc, u) => {
    acc[u.status] = (acc[u.status] ?? 0) + 1;
    return acc;
  }, {});
}

export function getGroupMemberCounts(): { name: string; count: number }[] {
  return GROUPS.map(g => ({ name: g.name, count: g.members.length }))
    .sort((a, b) => b.count - a.count);
}

export function getRiskLevelCounts(): Record<string, number> {
  return PERMISSIONS.reduce<Record<string, number>>((acc, p) => {
    acc[p.riskLevel] = (acc[p.riskLevel] ?? 0) + 1;
    return acc;
  }, {});
}
