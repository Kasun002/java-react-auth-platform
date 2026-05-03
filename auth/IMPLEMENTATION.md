# Auth Service — Full Implementation Reference

> **Project:** fp-be / auth microservice
> **Stack:** Java 21 · Spring Boot 4.0.6 · PostgreSQL · Flyway · JWT (jjwt 0.12.x) · Spring Mail · Spring Security 6
> **Last updated:** 2026-05-03

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Database Schema](#2-database-schema)
3. [API Endpoints](#3-api-endpoints)
4. [Registration Flow](#4-registration-flow)
5. [OTP Verification Flow](#5-otp-verification-flow)
6. [Login Flow](#6-login-flow)
7. [JWT Design](#7-jwt-design)
8. [RBAC Model](#8-rbac-model)
9. [Security Controls — Banking Standards Audit](#9-security-controls--banking-standards-audit)
10. [Exception Model](#10-exception-model)
11. [Configuration Reference](#11-configuration-reference)
12. [File Map](#12-file-map)
13. [Test Coverage](#13-test-coverage)
14. [Known Gaps & Future Work](#14-known-gaps--future-work)

---

## 1. Architecture Overview

```
Client
  │
  ▼
[JwtAuthenticationFilter]           ← OncePerRequestFilter: validates JWT, builds UserPrincipal
  │
  ▼
AuthController                      ← POST /auth/** (public — no token required)
AdminController                     ← GET|POST|DELETE /admin/** (@PreAuthorize on each method)
  │
  ├── AuthService / AuthServiceImpl  ← register, login, verifyOtp, resendOtp
  │       │
  │       ├── OtpService / OtpServiceImpl   ← OTP lifecycle: generate, verify, resend
  │       │       ├── OtpVerificationRepository  (JPA + pessimistic locks)
  │       │       ├── UserRepository
  │       │       └── EmailService / EmailServiceImpl  (Spring Mail / JavaMailSender)
  │       │
  │       ├── JwtService / JwtServiceImpl   ← Token creation & validation + claims extraction
  │       ├── UserGroupRepository            ← Auto-assign RETAIL_CUSTOMER on registration
  │       └── UserRepository, UserLogRepository, PasswordEncoder
  │
  ├── PermissionService / PermissionServiceImpl   ← List all permissions
  ├── BankingRoleService / BankingRoleServiceImpl ← Role CRUD, assign/remove permissions
  ├── UserGroupService / UserGroupServiceImpl     ← Group CRUD, user membership, effective permissions
  │
  └── GlobalExceptionHandler         ← Centralised error responses
```

All business exceptions extend `BusinessException` which carries an `HttpStatus`.
`GlobalExceptionHandler` translates them into a uniform `ResponseDto<Void>` JSON body.

---

## 2. Database Schema

### Flyway Migration Sequence

| Version | File | Purpose |
|---------|------|---------|
| V1 | `V1__create_users_table.sql` | Core users table |
| V2 | `V2__create_address_table.sql` | One-to-many addresses per user |
| V3 | `V3__create_user_log_table.sql` | Token audit log (initial) |
| V4 | `V4__add_role_to_users.sql` | ROLE enum column |
| V5 | `V5__enhance_user_log_table.sql` | Token type, issued_at, expires_at |
| V6 | `V6__add_lockout_to_users.sql` | Brute-force lockout columns |
| V7 | `V7__create_otp_verification_table.sql` | OTP records |
| V8 | `V8__add_otp_verification_indexes.sql` | Performance indexes for OTP queries |
| V9 | `V9__add_user_profile_fields.sql` | Optional profile columns on users table |
| V10 | `V10__create_rbac_tables.sql` | RBAC schema: 7 new tables + 4 indexes |
| V11 | `V11__seed_rbac_banking_data.sql` | 32 permissions, 12 roles, 13 groups + role→permission + group→role mappings |
| V12 | `V12__backfill_user_groups.sql` | Assigns existing USER accounts → RETAIL_CUSTOMER; ADMIN accounts → SYSTEM_ADMIN |

### `users` Table

```sql
id                    BIGSERIAL     PRIMARY KEY
name                  VARCHAR(255)  NOT NULL
email                 VARCHAR(255)  NOT NULL UNIQUE
phone                 VARCHAR(50)
password              VARCHAR(255)  NOT NULL        -- BCrypt hash, never plaintext
status                VARCHAR(50)   NOT NULL        -- NEW | ACTIVE | INACTIVE | DELETED
role                  VARCHAR(50)                   -- USER | ADMIN  (DEPRECATED — superseded by RBAC groups)
failed_login_attempts INT           NOT NULL DEFAULT 0
locked_until          TIMESTAMP     NULL            -- NULL = not locked
-- Optional profile fields (V9) — all nullable
date_of_birth         DATE          NULL
gender                VARCHAR(50)   NULL            -- MALE | FEMALE | OTHER | PREFER_NOT_TO_SAY
profile_picture_url   VARCHAR(1024) NULL
last_login_at         TIMESTAMP     NULL            -- updated on every successful login
created_at            TIMESTAMP     NOT NULL
updated_at            TIMESTAMP     NOT NULL
```

### RBAC Tables (V10)

```sql
-- permissions: atomic operation codes (e.g. ACCOUNT_VIEW, TRANSACTION_INITIATE)
permissions (id, code UNIQUE, description, category, created_at, updated_at)

-- banking_roles: named role definitions (e.g. ROLE_TELLER, ROLE_CUSTOMER_BASIC)
banking_roles (id, name UNIQUE, description, created_at, updated_at)

-- role_permissions: M:M — which permissions a role carries
role_permissions (role_id FK → banking_roles, permission_id FK → permissions)

-- user_groups: group definitions (e.g. RETAIL_CUSTOMER, BANK_TELLER)
user_groups (id, name UNIQUE, description, type, created_at, updated_at)
-- type: CUSTOMER | STAFF | OVERSIGHT | ADMIN

-- group_roles: M:M — roles assigned to a group
group_roles (group_id FK → user_groups, role_id FK → banking_roles)

-- user_group_memberships: M:M — which groups a user belongs to
user_group_memberships (user_id FK → users, group_id FK → user_groups, assigned_at)

-- user_role_assignments: M:M — direct role assignment (bypasses groups; for special cases)
user_role_assignments (user_id FK → users, role_id FK → banking_roles, assigned_at)
```

**Performance indexes (V10):** `idx_role_permissions_role`, `idx_group_roles_group`, `idx_user_group_user`, `idx_user_role_user`.

### `otp_verification` Table

```sql
id          BIGSERIAL PRIMARY KEY
user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE
otp_hash    VARCHAR(64)  NOT NULL    -- SHA-256 hex of the raw OTP; never the raw value
expires_at  TIMESTAMP    NOT NULL
attempts    INT          NOT NULL DEFAULT 0
used        BOOLEAN      NOT NULL DEFAULT FALSE
created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
```

**Indexes:**

```sql
idx_otp_user_id      ON otp_verification(user_id)
idx_otp_user_created ON otp_verification(user_id, created_at)             -- resend rate-limit query
idx_otp_user_unused  ON otp_verification(user_id, created_at DESC)
                     WHERE used = false                                    -- verify hot path
```

### `user_log` Table

Stores SHA-256 hashes of issued JWT tokens for audit purposes. Raw tokens are never persisted.

```
id, user_id, user_token (hash), token_type (ACCESS|REFRESH), issued_at, expires_at
```

---

## 3. API Endpoints

### Public — `/auth/**` (no token required)

| Method | Path | Status | Description |
|--------|------|--------|-------------|
| `POST` | `/auth/register` | 201 | Register user; OTP emailed |
| `POST` | `/auth/verify-otp` | 200 | Verify OTP; account → ACTIVE |
| `POST` | `/auth/resend-otp` | 200 | Resend OTP (max 3/hour) |
| `POST` | `/auth/login` | 200 | Authenticate; return JWT pair + UserDto |

### Admin — `/admin/**` (valid ACCESS token + `@PreAuthorize`)

| Method | Path | Permission | Description |
|--------|------|-----------|-------------|
| `GET` | `/admin/permissions` | `PERMISSION_MANAGE` | List all permissions |
| `GET` | `/admin/roles` | `ROLE_MANAGE` | List all roles with permissions |
| `GET` | `/admin/roles/{id}` | `ROLE_MANAGE` | Get role detail |
| `POST` | `/admin/roles/{id}/permissions` | `PERMISSION_MANAGE` | Assign permission to role |
| `DELETE` | `/admin/roles/{id}/permissions/{pid}` | `PERMISSION_MANAGE` | Remove permission from role → 204 |
| `GET` | `/admin/groups` | `GROUP_MANAGE` | List all groups |
| `GET` | `/admin/groups/{id}` | `GROUP_MANAGE` | Get group detail with roles |
| `POST` | `/admin/groups/{id}/roles` | `GROUP_MANAGE` | Assign role to group |
| `DELETE` | `/admin/groups/{id}/roles/{rid}` | `GROUP_MANAGE` | Remove role from group → 204 |
| `GET` | `/admin/users/{userId}/groups` | `USER_GROUPS_MANAGE` | Get user's group memberships |
| `POST` | `/admin/users/{userId}/groups` | `USER_GROUPS_MANAGE` | Add user to a group |
| `DELETE` | `/admin/users/{userId}/groups/{groupId}` | `USER_GROUPS_MANAGE` | Remove user from a group → 204 |
| `GET` | `/admin/users/{userId}/permissions` | `USER_GROUPS_MANAGE` | Get user's effective permission set |

All assign operations are **idempotent** — no error if already assigned. All remove operations are **idempotent** — no error if not present.

### `POST /auth/register` — Request Body

```json
{
  "name": "John Doe",
  "email": "john.doe@example.com",
  "phone": "+1234567890",
  "password": "SecurePass1!",
  "role": "USER",
  "addresses": [
    {
      "addressLine1": "123 Main St",
      "street": "Main Street",
      "postalCode": "10001",
      "state": "NY",
      "country": "USA"
    }
  ]
}
```

> **Note:** `status` field was removed (Step 4 security fix). Clients can no longer dictate their own account status. The service always sets `status = NEW`.

**Responses:** `201 Created` | `400 Bad Request` (validation) | `409 Conflict` (duplicate email)

**Side effects:** User saved with `status=NEW`, auto-assigned to `RETAIL_CUSTOMER` group. OTP generated and emailed.

### `POST /auth/login` — Success Response

```json
{
  "accessToken":  "<JWT>",
  "refreshToken": "<JWT>",
  "user": {
    "id": 42,
    "name": "John Doe",
    "email": "john.doe@example.com",
    "phone": "+94771234567",
    "status": "ACTIVE",
    "role": "USER",
    "dateOfBirth": "1990-06-15",
    "gender": "MALE",
    "profilePictureUrl": null,
    "lastLoginAt": "2026-05-03T11:00:00",
    "createdAt": "2026-01-10T08:30:00",
    "updatedAt": "2026-05-03T11:00:00",
    "groups": ["RETAIL_CUSTOMER"],
    "roles": ["ROLE_CUSTOMER_BASIC"],
    "effectivePermissions": ["ACCOUNT_VIEW", "TRANSACTION_VIEW", "TRANSACTION_INITIATE", "LOAN_VIEW", "LOAN_APPLY"],
    "addresses": [{ "addressLine1": "...", "country": "Sri Lanka" }]
  }
}
```

**Responses:** `200 OK` | `400` (validation) | `401` (wrong credentials / locked) | `403` (account not ACTIVE)

---

## 4. Registration Flow

```
POST /auth/register
        │
        ▼
  [Validation] @Valid on RegisterRequestDto
        │
        ▼
  [Duplicate check] userRepository.existsByEmail()
        │  ── email taken ──► 409 EmailAlreadyExistsException
        │
        ▼
  [Build User] status=NEW, password=BCrypt(password), role=USER (default)
        │
        ▼
  [Persist] userRepository.save(user)
        │
        ▼
  [Auto-assign default group]
  userGroupRepository.findByName("RETAIL_CUSTOMER").ifPresentOrElse(
      group -> { user.getGroups().add(group); userRepository.save(user); },
      () -> log.warn(...)   ← graceful degradation; never throws
  )
        │
        ▼
  [OTP: REQUIRES_NEW transaction]
        │   otpService.generateAndSend(user)
        │       ├── invalidateAllUnusedForUser(user)
        │       ├── generateRawOtp()                   ← SecureRandom, 000000–999999
        │       ├── save(OtpVerification{hash, expiry}) ← SHA-256 hash stored, not raw OTP
        │       └── emailService.sendOtp(...)
        │
        │  If emailService throws → REQUIRES_NEW rolls back OTP record only.
        │  User and group membership already committed. Caller must use /resend-otp.
        │
        ▼
  201 CREATED  { status: SUCCESS, message: "Registration successful. An OTP has been sent..." }
```

---

## 5. OTP Verification Flow

### 5.1 Verify (`POST /auth/verify-otp`)

```
[Input validation] @Pattern(\\d{6}) on otp field
        │
        ▼
  userRepository.findByEmail(email)
        │  ── not found ──► OtpInvalidException (same as wrong OTP — prevents enumeration)
        │
        ▼
  [Status guard]
        ├── ACTIVE   → return silently (idempotent)
        └── != NEW   → OtpInvalidException
        │
        ▼
  findTopByUserAndUsedFalseOrderByCreatedAtDesc(user)
  [PESSIMISTIC_WRITE LOCK — prevents concurrent race on attempt counter]
        │  ── no record ──► OtpExpiredException
        │
        ▼
  [Expiry check] LocalDateTime.now().isAfter(record.expiresAt)
        │  ── expired ──► OtpExpiredException
        │
        ▼
  [Attempt guard] record.attempts >= maxAttempts (3)
        │  ── limit hit ──► OtpMaxAttemptsException
        │
        ▼
  [Increment + save attempts BEFORE comparing hash]
  record.attempts++  →  otpVerificationRepository.save(record)
  [noRollbackFor=BusinessException ensures this counter commit survives a mismatch exception]
        │
        ▼
  [Constant-time hash comparison]
  MessageDigest.isEqual(
      sha256Hex(submittedOtp).getBytes(UTF-8),
      record.otpHash.getBytes(UTF-8)
  )
        │  ── mismatch ──► OtpInvalidException
        │
        ▼
  record.used = true
  user.status = ACTIVE
  save both
        │
        ▼
  200 OK  { status: SUCCESS, message: "Account verified successfully. You can now log in." }
```

### 5.2 Resend (`POST /auth/resend-otp`)

```
  userRepository.findByEmail(email)
        │  ── not found ──► OtpInvalidException
        │
        ▼
  [Status guard]
        ├── ACTIVE  → return silently
        └── != NEW  → OtpInvalidException
        │
        ▼
  countByUserAndCreatedAtAfter(user, now - 1 hour)
        │  ── count >= 3 ──► OtpResendLimitException
        │
        ▼
  generateAndSend(user)    [see Registration Flow — same internals]
        │
        ▼
  200 OK  { status: SUCCESS, message: "OTP resent successfully. Please check your email." }
```

---

## 6. Login Flow

```
POST /auth/login
        │
        ▼
  [Validation]  @Valid on LoginRequestDto
        │
        ▼
  Step 1: userRepository.findByEmail(username)
        │  ── not found ──► passwordEncoder.matches(dummy_hash) [TIMING EQUALIZER]
        │                    ──► 401 InvalidCredentialsException
        │
        ▼
  Step 2: [Lockout check] user.lockedUntil != null && now.isBefore(lockedUntil)
        │  ── locked ──► 401 AccountLockedException (message includes lock expiry)
        │
        ▼
  Step 3: [Password check] passwordEncoder.matches(request.password, user.password)
        │  ── mismatch ──► recordFailedAttempt(user)
        │                    [if attempts >= 5: lockedUntil = now + 30 min]
        │                    ──► 401 InvalidCredentialsException
        │
        ▼
  Step 4: [Status check] user.status != ACTIVE
        │  ── not active ──► 403 UserNotActiveException
        │  (checked AFTER password to avoid leaking account existence via status error)
        │
        ▼
  Step 5: Set user.lastLoginAt = now()
          Reset failedLoginAttempts + lockedUntil if non-zero
          userRepository.save(user)
        │
        ▼
  Step 6: generateAccessToken(user)   [15 min; embeds permissions + groups claims]
          generateRefreshToken(user)  [7 day]
        │
        ▼
  Step 7: persistUserLog — store SHA-256 hash of each token in user_log
        │
        ▼
  buildUserDto(user)  ← maps entity to UserDto including groups, roles, effectivePermissions
        │
        ▼
  200 OK  { accessToken, refreshToken, user: UserDto }
```

### `UserDto` — Field Reference

| Field | Type | Nullable | Notes |
|---|---|---|---|
| `id` | `Long` | no | DB primary key |
| `name` | `String` | no | Full display name |
| `email` | `String` | no | Unique identifier |
| `phone` | `String` | yes | Optional contact number |
| `status` | `UserStatus` | no | `NEW` \| `ACTIVE` \| `INACTIVE` \| `DELETED` |
| `role` | `Role` | yes | `USER` \| `ADMIN` — **deprecated**; use `roles` list |
| `dateOfBirth` | `LocalDate` | yes | Set via profile-edit API |
| `gender` | `Gender` | yes | `MALE` \| `FEMALE` \| `OTHER` \| `PREFER_NOT_TO_SAY` |
| `profilePictureUrl` | `String` | yes | CDN URL |
| `lastLoginAt` | `LocalDateTime` | yes | Updated on every successful login |
| `createdAt` | `LocalDateTime` | no | Account creation time |
| `updatedAt` | `LocalDateTime` | no | Last entity modification time |
| `groups` | `List<String>` | no | RBAC group names (e.g. `RETAIL_CUSTOMER`) |
| `roles` | `List<String>` | no | Role names from group + direct assignments |
| `effectivePermissions` | `List<String>` | no | Union of all permission codes |
| `addresses` | `List<AddressDto>` | yes | All registered addresses |

**Fields intentionally excluded:** `password`, `failedLoginAttempts`, `lockedUntil`.

### Brute-Force Lockout Rules

| Threshold | Action |
|-----------|--------|
| 1–4 failed attempts | Increment `failedLoginAttempts`, continue |
| 5th failed attempt | Lock account for 30 minutes |
| After lock expires | Next attempt proceeds normally; counter resets on success |

---

## 7. JWT Design

Tokens are signed with HMAC-SHA-256 using a key derived from `app.jwt.secret` (must be ≥ 256 bits).

### Claims

```json
{
  "jti":         "<UUID>",
  "iss":         "auth-service",
  "aud":         ["shop-platform"],
  "sub":         "john.doe@example.com",
  "userId":      42,
  "role":        "USER",
  "tokenType":   "ACCESS",
  "groups":      ["RETAIL_CUSTOMER"],
  "permissions": ["ACCOUNT_VIEW", "TRANSACTION_VIEW", "TRANSACTION_INITIATE", "LOAN_VIEW", "LOAN_APPLY"],
  "iat":         1746195600,
  "exp":         1746196500
}
```

- **`permissions`** — flat list of permission codes computed at login time from group roles + direct roles. Used directly by `@PreAuthorize("hasAuthority('ACCOUNT_VIEW')")` on downstream services without a DB call.
- **`groups`** — group names at time of login; informational.
- **`role`** — kept for backward compatibility; deprecated. Use `permissions` for authorization decisions.
- **`tokenType`** — `ACCESS` or `REFRESH`. The `JwtAuthenticationFilter` rejects REFRESH tokens for API calls (NIST 800-63B §7.1).

### Token Lifetimes (configurable)

| Token | Default | Config key |
|-------|---------|------------|
| Access | 15 min | `app.jwt.access-token-expiry-ms=900000` |
| Refresh | 7 days | `app.jwt.refresh-token-expiry-ms=604800000` |

### Token Validation (`JwtAuthenticationFilter`)

```
Request arrives
        │
        ▼
  [No Authorization header / not Bearer] → pass through (public endpoints)
        │
        ▼
  [jwtService.isTokenValid(token)] → false → 401 JSON
        │
        ▼
  [extractTokenType(token) != "ACCESS"] → 401 JSON (REFRESH token rejected)
        │
        ▼
  [Already authenticated in SecurityContext] → pass through (no double-processing)
        │
        ▼
  Extract email, userId, permissions, groups from claims
  Build UserPrincipal (implements UserDetails — no DB call)
  Set UsernamePasswordAuthenticationToken in SecurityContextHolder
        │
        ▼
  Continue filter chain
```

### Token Audit Log

Every issued token has its SHA-256 hash stored in `user_log` alongside `token_type`, `issued_at`, `expires_at`. Raw tokens are never persisted (PCI-DSS v4 Req 10.2).

---

## 8. RBAC Model

### Authorization Hierarchy

```
User
 ├── user_group_memberships (M:M) ──► UserGroup ──► group_roles (M:M) ──► BankingRole
 └── user_role_assignments  (M:M, direct) ──────────────────────────────► BankingRole
                                                                               │
                                                                  role_permissions (M:M)
                                                                               │
                                                                          Permission
                                                                     (code embedded in JWT)
```

**Effective permissions** = UNION of all permission codes reachable via group roles + direct roles. Computed at login time; embedded in the JWT `permissions` claim.

### Banking Groups (13)

| Group | Type | Default Roles |
|---|---|---|
| `RETAIL_CUSTOMER` | CUSTOMER | `ROLE_CUSTOMER_BASIC` |
| `PREMIUM_CUSTOMER` | CUSTOMER | `ROLE_CUSTOMER_BASIC`, `ROLE_CUSTOMER_PREMIUM` |
| `CORPORATE_CUSTOMER` | CUSTOMER | `ROLE_CUSTOMER_BASIC` |
| `BANK_TELLER` | STAFF | `ROLE_TELLER` |
| `LOAN_OFFICER` | STAFF | `ROLE_LOAN_PROCESSOR` |
| `RELATIONSHIP_MANAGER` | STAFF | `ROLE_RELATIONSHIP_MGR` |
| `BRANCH_MANAGER` | STAFF | `ROLE_BRANCH_MANAGER`, `ROLE_LOAN_APPROVER` |
| `OPERATIONS_STAFF` | STAFF | `ROLE_TELLER` |
| `COMPLIANCE_OFFICER` | OVERSIGHT | `ROLE_COMPLIANCE` |
| `RISK_ANALYST` | OVERSIGHT | `ROLE_RISK` |
| `INTERNAL_AUDITOR` | OVERSIGHT | `ROLE_AUDITOR` |
| `SYSTEM_ADMIN` | ADMIN | `ROLE_SYSTEM_ADMIN` |
| `SUPER_ADMIN` | ADMIN | `ROLE_SUPER_ADMIN` |

### Default Group on Registration

New users are automatically assigned to the `RETAIL_CUSTOMER` group in `AuthServiceImpl.register()`. If the seed group is not found (e.g., migration not run), a warning is logged and registration succeeds without group assignment — no exception is thrown.

### Permission Categories (32 total)

| Category | Examples |
|---|---|
| USER | `USER_VIEW`, `USER_CREATE`, `USER_UPDATE`, `USER_DEACTIVATE`, `USER_GROUPS_MANAGE` |
| ACCOUNT | `ACCOUNT_VIEW`, `ACCOUNT_CREATE`, `ACCOUNT_UPDATE`, `ACCOUNT_CLOSE` |
| TRANSACTION | `TRANSACTION_VIEW`, `TRANSACTION_INITIATE`, `TRANSACTION_APPROVE`, `TRANSACTION_REVERSE`, `TRANSACTION_HIGH_VALUE_APPROVE` |
| LOAN | `LOAN_VIEW`, `LOAN_APPLY`, `LOAN_APPROVE`, `LOAN_DISBURSE` |
| COMPLIANCE | `KYC_VIEW`, `KYC_APPROVE`, `KYC_REJECT`, `AML_FLAG_VIEW`, `AML_FLAG_RESOLVE` |
| REPORT | `REPORT_VIEW`, `REPORT_EXPORT`, `REPORT_AUDIT_VIEW` |
| ADMIN | `ROLE_MANAGE`, `GROUP_MANAGE`, `PERMISSION_MANAGE`, `SYSTEM_CONFIG_VIEW`, `SYSTEM_CONFIG_UPDATE`, `AUDIT_LOG_VIEW` |

### Spring Security Wiring

- `@EnableMethodSecurity(prePostEnabled = true)` on `SecurityConfig`
- `@PreAuthorize("hasAuthority('PERMISSION_CODE')")` on each `AdminController` method
- `UserPrincipal` (implements `UserDetails`) is built from JWT claims — **no DB call during request processing**
- `SecurityConfig` sets `SessionCreationPolicy.STATELESS` and wires custom 401/403 JSON handlers
- `ObjectMapper` in `SecurityConfig` is a `private static final` field — Spring Boot 4's auto-configuration ordering made constructor injection unreliable at context startup

---

## 9. Security Controls — Banking Standards Audit

### 9.1 OTP Generation

| Control | Implementation | Standard |
|---------|---------------|----------|
| Cryptographically secure randomness | `java.security.SecureRandom` | NIST 800-63B §5.1.4 |
| Full 10⁶ entropy (000000–999999) | `String.format("%06d", secureRandom.nextInt(1_000_000))` | NIST 800-63B §5.1.4 |
| OTP stored as hash only | SHA-256 hex stored; raw value sent over SMTP and never persisted | PCI-DSS v4 Req 8.3 |
| Short expiry | 10 minutes (configurable via `app.otp.expiry-minutes`) | NIST 800-63B §5.1.4.2 |

### 9.2 OTP Verification

| Control | Implementation | Standard |
|---------|---------------|----------|
| Constant-time comparison | `MessageDigest.isEqual(hash1.getBytes(), hash2.getBytes())` | OWASP ASVS 2.7.6 |
| Attempt limiting (3 per OTP) | Counter incremented *before* hash comparison; `noRollbackFor` ensures commit on exception | NIST 800-63B §5.1.4.2 |
| Pessimistic DB lock on OTP record | `@Lock(PESSIMISTIC_WRITE)` on repository query | OWASP ASVS 11.1.6 |
| Previous OTPs invalidated on resend | `invalidateAllUnusedForUser()` called at start of `generateAndSend()` | NIST 800-63B §5.1.4.2 |

### 9.3 Login Security

| Control | Implementation | Standard |
|---------|---------------|----------|
| Password hashing | BCrypt (`BCryptPasswordEncoder`) | NIST 800-63B §5.1.1.2 |
| Timing attack prevention | Dummy BCrypt hash check when user not found | OWASP ASVS 2.2.2 |
| Brute-force lockout | 5 failed attempts → 30-minute lockout | NIST 800-63B §5.2.2 |
| Status check after password | Account status only revealed after correct password | OWASP ASVS 2.1.5 |
| Failed attempt tracking | `noRollbackFor=BusinessException` ensures counter commits on exception | PCI-DSS v4 Req 8.3 |
| `status` removed from `RegisterRequestDto` | Client cannot set own account status | OWASP ASVS 4.1.2 |

### 9.4 JWT & Token Security

| Control | Implementation | Standard |
|---------|---------------|----------|
| Signed with HMAC-SHA-256 | `Keys.hmacShaKeyFor(secret.getBytes(UTF-8))` | RFC 7518 |
| Unique token ID (jti) | `UUID.randomUUID()` per token — enables per-token revocation | OWASP ASVS 3.5.1 |
| Audience claim | `aud: ["shop-platform"]` — prevents cross-service replay | RFC 7519 §4.1.3 |
| Token audit log | SHA-256 hash stored in `user_log` | PCI-DSS v4 Req 10.2 |
| Short access token lifetime | 15 minutes | OWASP ASVS 3.3.1 |
| REFRESH tokens rejected for API calls | `JwtAuthenticationFilter` checks `tokenType == ACCESS` | NIST 800-63B §7.1 |
| Permission snapshot in token | Computed at login; re-login required on permission change | PCI-DSS v4 Req 8.3.9 |

### 9.5 RBAC Access Control

| Control | Implementation | Standard |
|---------|---------------|----------|
| Least privilege | New users default to `RETAIL_CUSTOMER` (customer-only permissions) | ISO 27001 A.9.2.3 |
| Separation of duties | `TRANSACTION_HIGH_VALUE_APPROVE` is a separate permission; auditors are read-only | ISO 27001 A.9.4.2 |
| No permission escalation | Users cannot assign themselves to groups; `USER_GROUPS_MANAGE` required | OWASP ASVS 4.1.2 |
| Access review support | `GET /admin/users/{id}/permissions` enables periodic access reviews | PCI-DSS v4 Req 7.2 |
| All admin actions auditable | All `/admin/**` calls logged via `RequestLoggingFilter` + MDC correlation ID | PCI-DSS v4 Req 10.2 |
| No sensitive data in JWT | JWT contains only permission codes, not account balances or PII | OWASP ASVS 3.5.2 |

### 9.6 Logging & Non-Disclosure

All log statements mask email addresses via `MaskingUtil.maskEmail()` (e.g., `jo**@example.com`).
OTP values and raw tokens are **never** logged at any level.
Error responses never expose internal stack traces.

---

## 10. Exception Model

```
BusinessException  (abstract — carries HttpStatus)
  ├── EmailAlreadyExistsException     409 CONFLICT
  ├── InvalidCredentialsException     401 UNAUTHORIZED
  ├── AccountLockedException          401 UNAUTHORIZED   (message includes lock expiry)
  ├── UserNotActiveException          403 FORBIDDEN
  ├── OtpInvalidException             400 BAD_REQUEST    (also used for unknown email)
  ├── OtpExpiredException             400 BAD_REQUEST
  ├── OtpMaxAttemptsException         429 TOO_MANY_REQUESTS
  ├── OtpResendLimitException         429 TOO_MANY_REQUESTS
  ├── ResourceNotFoundException       404 NOT_FOUND      (RBAC: role/group/user not found)
  └── AccessDeniedException           403 FORBIDDEN      (explicit service-layer 403)

JwtAuthenticationException  (extends RuntimeException — handled by filter, not GlobalExceptionHandler)
  └── Written directly as 401 JSON by JwtAuthenticationFilter
```

### Uniform Response Shape

```json
{
  "status":  "SUCCESS" | "FAIL",
  "message": "...",
  "data":    { ... },      // present on success, null on error
  "errors":  [ "..." ]     // present on validation failure (400 from @Valid)
}
```

---

## 11. Configuration Reference

```properties
# ── Database ──────────────────────────────────────────────────────────────────
spring.datasource.url=jdbc:postgresql://localhost:5433/auth_db
spring.datasource.username=admin
spring.datasource.password=admin

# ── Mail (override via env vars in production) ────────────────────────────────
spring.mail.host=${MAIL_HOST:smtp.gmail.com}
spring.mail.port=${MAIL_PORT:587}
spring.mail.username=${MAIL_USERNAME:noreply@shop.com}
spring.mail.password=${MAIL_PASSWORD:}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# ── OTP ───────────────────────────────────────────────────────────────────────
app.otp.expiry-minutes=10          # OTP valid for N minutes after generation
app.otp.max-attempts=3             # Failed attempts before OTP is locked
app.otp.max-resends-per-hour=3     # Max resend requests per user per hour

# ── JWT ───────────────────────────────────────────────────────────────────────
# IMPORTANT: Replace JWT_SECRET with a 256+ bit random value in production
app.jwt.secret=${JWT_SECRET:MyVeryLongAndSecureJwtSecretKeyThatIsAtLeast256BitsLongForDevOnly}
app.jwt.access-token-expiry-ms=900000      # 15 minutes
app.jwt.refresh-token-expiry-ms=604800000  # 7 days
```

### Production Checklist

- [ ] Set `JWT_SECRET` to a cryptographically random ≥ 32-byte value (env var or vault secret)
- [ ] Set `MAIL_USERNAME` / `MAIL_PASSWORD` (or use AWS SES / SendGrid)
- [ ] Set `MAIL_HOST` / `MAIL_PORT` to match your SMTP provider
- [ ] Disable `spring.jpa.show-sql` and `logging.level.org.hibernate.SQL=DEBUG`
- [ ] Enable HTTPS / TLS at the load balancer or ingress
- [ ] Verify V11 seed data present: `SELECT count(*) FROM permissions` → 32; `SELECT count(*) FROM user_groups` → 13
- [ ] Run V12 migration and verify: `SELECT count(*) FROM user_group_memberships` > 0

---

## 12. File Map

```
auth/
├── src/main/java/com/shop/auth/
│   ├── controller/
│   │   ├── AuthController.java              # POST /auth/register, /login, /verify-otp, /resend-otp
│   │   └── AdminController.java             # 13 admin endpoints under /admin/**
│   ├── filter/
│   │   └── JwtAuthenticationFilter.java     # OncePerRequestFilter: validates JWT, sets UserPrincipal
│   ├── security/
│   │   └── UserPrincipal.java               # Implements UserDetails; built from JWT claims (no DB)
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── OtpService.java
│   │   ├── EmailService.java
│   │   ├── JwtService.java                  # + extractPermissions, extractGroups, extractTokenType, isTokenValid(String)
│   │   ├── PermissionService.java           # listAll()
│   │   ├── BankingRoleService.java          # listAll, getById, assignPermission, removePermission
│   │   ├── UserGroupService.java            # listAll, getById, assignRoleToGroup, removeRoleFromGroup,
│   │   │                                    #   getUserGroups, addUserToGroup, removeUserFromGroup,
│   │   │                                    #   getEffectivePermissions
│   │   └── impl/
│   │       ├── AuthServiceImpl.java         # register() auto-assigns RETAIL_CUSTOMER; buildUserDto() includes RBAC fields
│   │       ├── OtpServiceImpl.java
│   │       ├── EmailServiceImpl.java
│   │       ├── JwtServiceImpl.java          # buildToken() embeds permissions + groups claims
│   │       ├── PermissionServiceImpl.java
│   │       ├── BankingRoleServiceImpl.java  # @Transactional(readOnly=true) + idempotent mutations
│   │       └── UserGroupServiceImpl.java    # getEffectivePermissions traverses lazy collections
│   ├── entity/
│   │   ├── User.java                        # + groups (M:M UserGroup), directRoles (M:M BankingRole)
│   │   ├── Address.java
│   │   ├── UserLog.java
│   │   ├── OtpVerification.java
│   │   ├── Permission.java                  # @EqualsAndHashCode(onlyExplicitlyIncluded=true) on id
│   │   ├── BankingRole.java                 # + Set<Permission> via role_permissions
│   │   └── UserGroup.java                   # + Set<BankingRole> via group_roles
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── UserLogRepository.java
│   │   ├── OtpVerificationRepository.java
│   │   ├── PermissionRepository.java        # findByCode(String)
│   │   ├── BankingRoleRepository.java       # findByName(String)
│   │   └── UserGroupRepository.java         # findByName(String)
│   ├── dto/
│   │   ├── RegisterRequestDto.java          # status field removed (security fix)
│   │   ├── LoginRequestDto.java
│   │   ├── LoginResponseDto.java
│   │   ├── UserDto.java                     # + groups, roles, effectivePermissions
│   │   ├── AddressDto.java
│   │   ├── VerifyOtpRequestDto.java
│   │   ├── ResendOtpRequestDto.java
│   │   ├── ResponseDto.java
│   │   ├── PermissionDto.java               # id, code, description, category
│   │   ├── BankingRoleDto.java              # id, name, description, List<PermissionDto>
│   │   ├── UserGroupDto.java                # id, name, description, type, List<BankingRoleDto>
│   │   ├── AssignPermissionToRoleRequestDto.java  # @NotNull Long permissionId
│   │   ├── AssignRoleToGroupRequestDto.java       # @NotNull Long roleId
│   │   └── AssignGroupRequestDto.java             # @NotNull Long groupId
│   ├── exception/
│   │   ├── BusinessException.java
│   │   ├── EmailAlreadyExistsException.java  # 409
│   │   ├── InvalidCredentialsException.java  # 401
│   │   ├── AccountLockedException.java       # 401
│   │   ├── UserNotActiveException.java       # 403
│   │   ├── OtpInvalidException.java          # 400
│   │   ├── OtpExpiredException.java          # 400
│   │   ├── OtpMaxAttemptsException.java      # 429
│   │   ├── OtpResendLimitException.java      # 429
│   │   ├── ResourceNotFoundException.java    # 404
│   │   ├── AccessDeniedException.java        # 403 (explicit service-layer)
│   │   ├── JwtAuthenticationException.java   # RuntimeException — handled by filter
│   │   └── handler/
│   │       └── GlobalExceptionHandler.java
│   ├── config/
│   │   └── SecurityConfig.java              # @EnableMethodSecurity; JWT filter; stateless; 401/403 JSON handlers
│   │                                        # ObjectMapper is a private static final field (not injected) —
│   │                                        # avoids Spring Boot 4 auto-config ordering issue at startup
│   └── utils/
│       ├── MaskingUtil.java
│       ├── HashUtil.java                    # sha256Hex(String) — shared by AuthServiceImpl + OtpServiceImpl
│       ├── Role.java                        # @Deprecated — superseded by RBAC group model
│       ├── UserStatus.java
│       ├── Gender.java
│       └── TokenType.java
│
├── src/main/resources/
│   ├── application.properties
│   └── db/migration/
│       ├── V1 – V9  (unchanged)
│       ├── V10__create_rbac_tables.sql
│       ├── V11__seed_rbac_banking_data.sql
│       └── V12__backfill_user_groups.sql
│
└── src/test/java/com/shop/auth/
    ├── controller/
    │   ├── AuthControllerTest.java          # 27 tests — all 4 public auth endpoints
    │   └── AdminControllerTest.java         # 23 tests — all 13 admin endpoints; standalone MockMvc
    ├── filter/
    │   └── JwtAuthenticationFilterTest.java # 10 tests — no header, valid token, invalid, REFRESH, authorities
    ├── service/impl/
    │   ├── AuthServiceImplTest.java         # 14 tests — registration (StatusEnforcement removed)
    │   ├── AuthServiceImplLoginTest.java    # 26 tests — login: tokens, lockout, status, lastLoginAt
    │   ├── OtpServiceImplTest.java          # 18 tests — full OTP lifecycle
    │   ├── BankingRoleServiceImplTest.java  # 11 tests — listAll, getById, assignPermission, removePermission
    │   └── UserGroupServiceImplTest.java    # 15 tests — listAll, getById, assignRole, getUserGroups,
    │                                        #             addUserToGroup, getEffectivePermissions
    ├── fixtures/
    │   ├── RegisterRequestDtoFixture.java   # withStatus() removed
    │   ├── LoginRequestDtoFixture.java
    │   └── AddressDtoFixture.java
    └── MaskingUtilTest.java                 # 13 tests
```

---

## 13. Test Coverage

Total: **165 tests, 0 failures**.

| Test Class | Tests | Key Scenarios |
|---|---|---|
| `MaskingUtilTest` | 13 | Email/phone masking patterns |
| `AuthControllerTest` | 27 | Validation, business errors, success paths for all 4 endpoints |
| `AdminControllerTest` | 23 | 200/204/400/404 for all 13 admin endpoints; standalone MockMvc |
| `JwtAuthenticationFilterTest` | 10 | No header passes through, valid token sets context, invalid/expired/REFRESH → 401, authorities and groups populated |
| `AuthServiceImplTest` | 14 | Registration: password hashing, role default, address linking, duplicate email |
| `AuthServiceImplLoginTest` | 26 | Login: tokens, UserDto RBAC fields, `lastLoginAt`, lockout, status guards, counter reset |
| `OtpServiceImplTest` | 18 | Generate+send, verify (success/mismatch/expired/maxAttempts/idempotent), resend (rate-limit/invalidation) |
| `BankingRoleServiceImplTest` | 11 | listAll, getById, assignPermission (idempotent + 404), removePermission (idempotent + 404) |
| `UserGroupServiceImplTest` | 15 | listAll, getById, assignRole (idempotent), getUserGroups, addUserToGroup (idempotent), getEffectivePermissions (union + dedup + empty) |
| `GlobalExceptionHandlerTest` | 8 | BusinessException, validation, malformed JSON, generic 500 |
| `AuthApplicationTests` | 1 | Spring context loads |

### Design Principles Applied Across All Tests

- `MockitoSettings(strictness = STRICT_STUBS)` — unused stubs fail the test
- `MockMvcBuilders.standaloneSetup()` — bypasses Spring Security filter chain; no `@WithMockUser` needed
- `@InjectMocks` / `@Mock` — no Spring context loading; pure unit tests
- Idempotency verified with `verify(repo, never()).save(any())` on the no-op paths

---

## 14. Known Gaps & Future Work

### 14.1 Spring Self-Invocation in `resend()`

`OtpServiceImpl.resend()` calls `this.generateAndSend(user)`. Because `generateAndSend` is annotated `@Transactional(REQUIRES_NEW)`, the intent is to run it in its own nested transaction. However, Spring AOP proxies are bypassed on self-invocation — `REQUIRES_NEW` is silently ignored and the call runs within the outer `resend()` `REQUIRED` transaction.

**Practical impact (LOW):** The entire `resend()` operation is atomic. If email fails, all roll back — the resend quota is not consumed. This is functionally correct.

**Future fix:** Extract `generateAndSend` logic into a separate `OtpGeneratorService` bean.

### 14.2 Email Delivery Reliability

Currently relies on synchronous SMTP. In production:
- Use an async queue (SQS + SES, or RabbitMQ + SendGrid)
- Add retry with exponential backoff for transient SMTP failures
- Add dead-letter handling for permanently failed deliveries

### 14.3 OTP Cleanup Job

`otp_verification` rows are never deleted. A scheduled job should periodically purge records older than N days (e.g., 30 days).

### 14.4 Profile Edit API

`User` and `UserDto` carry optional profile fields ready for a `PATCH /profile` endpoint. When implemented:
- `AddressDto` will need an `id` field for individual address updates.
- `profilePictureUrl` should be restricted to trusted CDN domains.
- Password change should be a separate endpoint requiring `currentPassword` verification.

### 14.5 Refresh Token Endpoint

`/auth/refresh` to exchange a valid refresh token for a new access token is not yet implemented. The `user_log` table and `jti` claim support revocation.

### 14.6 V13 — Deprecate `users.role` Column

`Role.java` is already annotated `@Deprecated`. When fully migrated, run:

```sql
ALTER TABLE users RENAME COLUMN role TO role_legacy;
COMMENT ON COLUMN users.role_legacy IS 'DEPRECATED: superseded by user_group_memberships. Remove after 2027-01-01.';
```

Then annotate `User.role` field with `@Deprecated` and `@Column(name = "role_legacy")`.

### 14.7 CSRF

CSRF is disabled in `SecurityConfig`. This is appropriate for a stateless JWT API (no session cookies), but should be documented in security reviews.

### 14.8 Rate Limiting at API Gateway Level

The per-user OTP resend rate limit (3/hour) is enforced in the application layer. For DDoS resilience, add IP-level rate limiting at the API gateway / ingress layer.


