# java-react-auth-platform

Enterprise grade authentication platform — JWT auth service with Azure AD/LDAP SSO, RBAC, OTP verification, and a React frontend.

---

## Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 4, PostgreSQL, Redis, Flyway |
| Auth | JWT (HS512), Spring Security 6, Spring LDAP |
| SSO | Keycloak (dev) / Azure AD (prod), OIDC + PKCE |
| Async | AWS SQS + SES (LocalStack in dev) |
| Frontend | React 19, Vite, TailwindCSS, Axios |
| Infrastructure | Docker Compose |

---

## Project Structure

```
fp-be/
├── auth/               # Spring Boot auth microservice (port 8080)
├── auth-fe/            # React frontend (port 5173)
├── docker-compose.yml  # All infrastructure services
└── README.md
```

---

## Prerequisites

- Java 21
- Maven 3.9+
- Node.js 20+
- Docker Desktop

---

## 1. Start Infrastructure

From the repository root:

```bash
docker compose up -d
```

| Service | URL | Credentials |
|---|---|---|
| PostgreSQL (auth) | `localhost:5433` | `admin / admin` |
| PostgreSQL (product) | `localhost:5434` | `admin / admin` |
| Redis | `localhost:6379` | — |
| pgAdmin | http://localhost:5050 | `admin@example.com / admin` |
| Keycloak | http://localhost:8180 | `admin / admin` |
| OpenLDAP | `localhost:389` | `cn=admin,dc=corp,dc=example,dc=com / admin` |
| phpLDAPadmin | http://localhost:6443 | — |

---

## 2. LocalStack Setup (one-time per container start)

LocalStack emulates AWS SQS and SES locally. After `docker compose up -d`, the SQS queue is auto-created, but the SES sender identity must be verified manually — LocalStack resets it on every container restart.

```bash
aws --endpoint-url=http://localhost:4566 ses verify-email-identity \
  --email-address noreply@shop.com --region us-east-1
```

Confirm it worked:

```bash
aws --endpoint-url=http://localhost:4566 ses list-identities --region us-east-1
# Expected: { "Identities": ["noreply@shop.com"] }
```

> **Note:** OTP emails will fail with `MessageRejectedException: Email address not verified` if you skip this step or restart the LocalStack container without re-running the command.

---

## 3. Keycloak Setup (one-time)

> Data persists across restarts thanks to `dev-file` storage.

**1. Open** http://localhost:8180 → log in as `admin / admin`

**2. Create realm**
- Click realm dropdown (top-left) → **Create realm**
- Name: `corporate` → **Create**

**3. Create client** — stay in `corporate` realm
- **Clients** → **Create client**
- Client ID: `fp-auth-client` → **Next**
- Turn ON **Direct access grants** → **Next** → **Save**
- **Settings** tab → Valid redirect URIs: add `http://localhost:5173/*` and `http://localhost:5174/*`
- Web origins: `http://localhost:5173` → **Save**

**4. Create test users** — repeat for each:

| Email | First name | Last name | Password |
|---|---|---|---|
| `alice@corp.example.com` | Alice | Retail | `Alice@Pass1!` |
| `bob@corp.example.com` | Bob | Staff | `Bob@Pass1!` |
| `carol@corp.example.com` | Carol | Admin | `Carol@Pass1!` |

For each: **Users** → **Add user** → fill email (set as username) + first/last name → **Email verified ON** → **Create**
Then **Credentials** tab → **Set password** → enter password → **Temporary OFF** → **Save password**

---

## 4. Start the Auth Service

```bash
cd auth
mvn spring-boot:run
```

API base URL: http://localhost:8080
Swagger UI: http://localhost:8080/swagger-ui.html

> AD login is enabled by default in `application.properties`. No extra config needed for local dev.

---

## 5. Start the Frontend

```bash
cd auth-fe
npm install
npm run dev
```

App URL: http://localhost:5173

---

## 6. Verify the Full Flow

**Local login** — register a user first via Swagger or:
```bash
curl -s -X POST http://localhost:8080/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"name":"Test User","email":"test@example.com","password":"Secure@Pass1!","addresses":[{"addressLine1":"123 Main St","country":"Sri Lanka"}]}' | jq .
```

**AD login** — get a Keycloak token then exchange:
```bash
ID_TOKEN=$(curl -s -X POST http://localhost:8180/realms/corporate/protocol/openid-connect/token \
  -d 'grant_type=password' -d 'client_id=fp-auth-client' -d 'scope=openid' \
  -d 'username=alice@corp.example.com' -d 'password=Alice@Pass1!' | jq -r '.id_token')

curl -s -X POST http://localhost:8080/auth/ad/login \
  -H 'Content-Type: application/json' \
  -d "{\"idToken\":\"$ID_TOKEN\"}" | jq .
```

Expected: `"status": "SUCCESS"` with `accessToken` and `refreshToken`.

---

## 7. Granting Admin Access

The admin frontend (`/dashboard`, `/users`, `/groups`, `/roles`, `/permissions`, `/audit`, `/settings`) requires specific RBAC permissions. Permissions are delivered through groups — a user must be a member of `SYSTEM_ADMIN` or `SUPER_ADMIN` to use admin pages.

### How it works

```
Keycloak / LDAP group          ad_group_mappings table       local UserGroup
─────────────────────          ───────────────────────       ────────────────
GRP-SYSTEM-ADMINS    ────────► SYSTEM_ADMIN           ────► ROLE_SYSTEM_ADMIN
GRP-SYSTEM-ADMINS    ────────► SUPER_ADMIN             ────► ROLE_SUPER_ADMIN
```

On every AD login the auth service reads the user's LDAP groups, looks them up in `ad_group_mappings`, and places the user into the corresponding local groups. The resulting permissions are embedded in the JWT.

### Permissions required per admin page

| Page | Required permission |
|---|---|
| `/dashboard` | `DASHBOARD_VIEW` |
| `/users` | `USER_GROUPS_MANAGE` |
| `/groups` | `GROUP_MANAGE` |
| `/roles` | `ROLE_MANAGE` |
| `/permissions` | `PERMISSION_MANAGE` |
| `/audit` | `AUDIT_LOG_VIEW` |
| `/settings` | `SYSTEM_CONFIG_VIEW` |

`ROLE_SYSTEM_ADMIN` carries all of the above. `ROLE_SUPER_ADMIN` carries every permission in the system.

---

### Option A — Keycloak / AD SSO user (recommended)

This is the correct path for any user who logs in via `POST /auth/ad/login`.

#### Step 1 — Add the user to `GRP-SYSTEM-ADMINS` in OpenLDAP

`carol@corp.example.com` is already seeded into this group by `auth/docker/ldap/bootstrap.ldif` — skip to Step 2 for Carol.

For any other user, add them via **phpLDAPadmin** (http://localhost:6443, login `cn=admin,dc=corp,dc=example,dc=com` / `admin`):

1. Expand **`ou=users`** → **Create a child entry** → `Generic: User Account`
2. Fill in `cn`, `sn`, `mail` (= login email), `userPassword`
3. Navigate to **`ou=groups → cn=GRP-SYSTEM-ADMINS`** → **Add new attribute** → `member` → enter the user's DN (`cn=Full Name,ou=users,dc=corp,dc=example,dc=com`)

Or via `ldapmodify` on the command line:

```bash
# Replace "New User" and email with the real values
cat > /tmp/add-admin.ldif << 'EOF'
dn: cn=New User,ou=users,dc=corp,dc=example,dc=com
changetype: add
objectClass: inetOrgPerson
cn: New User
sn: User
mail: newuser@corp.example.com
userPassword: NewUser@Pass1!

dn: cn=GRP-SYSTEM-ADMINS,ou=groups,dc=corp,dc=example,dc=com
changetype: modify
add: member
member: cn=New User,ou=users,dc=corp,dc=example,dc=com
EOF

ldapmodify -x -H ldap://localhost:389 \
  -D "cn=admin,dc=corp,dc=example,dc=com" -w admin \
  -f /tmp/add-admin.ldif
```

Also create the matching Keycloak account (same email, Step 4 of the Keycloak setup above).

#### Step 2 — AD group mappings (already seeded by migration V19)

Flyway migration `V19__seed_ad_group_mappings.sql` runs automatically on startup and creates these mappings:

| LDAP group (`ad_group_id`) | Local group |
|---|---|
| `GRP-RETAIL-CUSTOMERS` | `RETAIL_CUSTOMER` |
| `GRP-BANK-STAFF` | `BANK_TELLER` |
| `GRP-SYSTEM-ADMINS` | `SYSTEM_ADMIN` |

No manual SQL is needed. You can verify the mappings in pgAdmin or with:

```bash
PGPASSWORD=admin psql -h localhost -p 5433 -U admin -d auth_db -c \
  "SELECT m.ad_group_id, g.name AS local_group FROM ad_group_mappings m JOIN user_groups g ON g.id = m.local_group_id;"
```

To map `GRP-SYSTEM-ADMINS` to `SUPER_ADMIN` instead (full access), update the row:

```sql
UPDATE ad_group_mappings
SET    local_group_id = (SELECT id FROM user_groups WHERE name = 'SUPER_ADMIN')
WHERE  ad_group_id = 'GRP-SYSTEM-ADMINS';
```

> `ad_group_id` must exactly match the LDAP `cn` of the group (case-sensitive).
> The `unmapped-group-strategy` defaults to `DEFAULT`, which falls back to `RETAIL_CUSTOMER` for any LDAP group that has **no** mapping entry — so unmapped users cannot reach admin pages.

#### Step 3 — Log in as the admin user

```bash
# Get a Keycloak ID token for Carol
ID_TOKEN=$(curl -s -X POST http://localhost:8180/realms/corporate/protocol/openid-connect/token \
  -d 'grant_type=password' \
  -d 'client_id=fp-auth-client' \
  -d 'scope=openid' \
  -d 'username=carol@corp.example.com' \
  -d 'password=Carol@Pass1!' | jq -r '.id_token')

# Exchange it for a service JWT — this triggers LDAP group sync
curl -s -X POST http://localhost:8080/auth/ad/login \
  -H 'Content-Type: application/json' \
  -d "{\"idToken\":\"$ID_TOKEN\"}" | jq .
```

Expected response includes `"status": "SUCCESS"` with an `accessToken`. Decode the JWT payload to confirm the group and permissions:

```bash
ACCESS_TOKEN="<paste accessToken here>"
echo "$ACCESS_TOKEN" | cut -d. -f2 \
  | awk '{n=length($0)%4; if(n==2)pad="=="; else if(n==3)pad="="; else pad=""; print $0 pad}' \
  | base64 -d 2>/dev/null | jq '{groups, permissions}'
```

Expected output:
```json
{
  "groups": ["SYSTEM_ADMIN"],
  "permissions": [
    "DASHBOARD_VIEW", "GROUP_MANAGE", "ROLE_MANAGE", "PERMISSION_MANAGE",
    "USER_GROUPS_MANAGE", "AUDIT_LOG_VIEW", "SYSTEM_CONFIG_VIEW",
    "USER_VIEW", "USER_CREATE", "USER_UPDATE", "USER_DEACTIVATE", "SYSTEM_CONFIG_UPDATE"
  ]
}
```

#### LDAP bind account — local dev note

`application.properties` defaults to binding with `cn=admin,dc=corp,dc=example,dc=com` (the OpenLDAP admin) because `osixia/openldap` does not grant regular LDAP entries read access to other subtrees by default. The `svc-ldap` service account defined in `bootstrap.ldif` is provided for **production** use where a sysadmin will configure the appropriate LDAP ACLs.

In production, override via environment variables:
```bash
AD_LDAP_USER_DN=cn=svc-ldap,ou=service,dc=corp,dc=example,dc=com
AD_LDAP_PASSWORD=<vault-secret>
```

---

### Option B — Locally registered user (no SSO)

For a user registered via `POST /auth/register` or created directly in the database.

#### Via SQL (quickest for local dev)

```sql
-- Connect to auth_db and run:
INSERT INTO user_group_memberships (user_id, group_id)
SELECT u.id, g.id
FROM   users u
JOIN   user_groups g ON g.name = 'SYSTEM_ADMIN'   -- or 'SUPER_ADMIN'
WHERE  u.email = 'youruser@example.com';
```

The user's next login will issue a JWT with full admin permissions.

#### Via Admin API

First obtain an existing admin `accessToken` (e.g. from Carol in Option A), then:

```bash
ADMIN_TOKEN="<paste accessToken here>"

# 1. Find the user's numeric ID
# Check pgAdmin or use: GET /admin/... (not yet exposed — use pgAdmin or SQL below)
USER_ID=$(psql -h localhost -p 5433 -U admin -d auth_db -tAc \
  "SELECT id FROM users WHERE email = 'youruser@example.com'")

# 2. Find the SYSTEM_ADMIN group ID
GROUP_ID=$(psql -h localhost -p 5433 -U admin -d auth_db -tAc \
  "SELECT id FROM user_groups WHERE name = 'SYSTEM_ADMIN'")

# 3. Assign
curl -s -X POST "http://localhost:8080/admin/users/${USER_ID}/groups" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d "{\"groupId\": ${GROUP_ID}}" | jq .
```

---

### Verifying permissions

After login, decode the `accessToken` at https://jwt.io or with:

```bash
# Paste your access token after "Bearer "
TOKEN="<accessToken>"
echo "$TOKEN" | cut -d. -f2 | base64 -d 2>/dev/null | jq .permissions
```

You should see `["DASHBOARD_VIEW", "GROUP_MANAGE", "ROLE_MANAGE", ...]` (or more for `SUPER_ADMIN`).

---

### Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `"groups": []` and `"permissions": []` in JWT | LDAP groups not resolving | Check service log for `LDAP group lookup failed` — usually an ACL or bind-DN issue |
| `LDAP: error code 32 - No Such Object` | LDAP bind account lacks read access to `ou=users` / `ou=groups` | Default `application.properties` already uses OpenLDAP admin; ensure `AD_LDAP_USER_DN` / `AD_LDAP_PASSWORD` are not overriding to `svc-ldap` locally |
| `LDAP: error code 49 - Invalid credentials` | Wrong bind password | Confirm `AD_LDAP_PASSWORD=admin` for local dev (OpenLDAP admin password from `docker-compose.yml`) |
| Login succeeds but dashboard returns `403` | `ad_group_mappings` row missing | Verify V19 ran: `SELECT * FROM ad_group_mappings;` — restart service if table is empty |
| `"No LDAP groups returned"` in log | User not a member of any mapped LDAP group | Confirm the user is in `GRP-SYSTEM-ADMINS` via `ldapsearch` (see Step 1) |

### Quick reference — group → role → key permissions

| Local group | Role | Key permissions |
|---|---|---|
| `RETAIL_CUSTOMER` | `ROLE_CUSTOMER_BASIC` | `ACCOUNT_VIEW`, `TRANSACTION_VIEW` |
| `SYSTEM_ADMIN` | `ROLE_SYSTEM_ADMIN` | `DASHBOARD_VIEW`, `GROUP_MANAGE`, `ROLE_MANAGE`, `PERMISSION_MANAGE`, `USER_GROUPS_MANAGE`, `AUDIT_LOG_VIEW`, `SYSTEM_CONFIG_VIEW` |
| `SUPER_ADMIN` | `ROLE_SUPER_ADMIN` | **all permissions** |

---

## 8. Environment Variables

All services use sensible local defaults — no `.env` files needed for local dev.

| Variable | Default | Description |
|---|---|---|
| `JWT_SECRET` | (dev key) | Min 32-byte secret — **change in production** |
| `AD_ENABLED` | `true` | Enable AD/OIDC login |
| `AD_LDAP_PASSWORD` | `svc-password` | LDAP service account password |
| `CORS_ALLOWED_ORIGINS` | `localhost:3000,4200,5173,5174` | Frontend origins |
| `REDIS_HOST` | `localhost` | Redis host |
| `AWS_ENDPOINT` | `http://localhost:4566` | LocalStack endpoint (blank = real AWS) |

Frontend env (`.env` in `auth-fe/`):

```properties
VITE_API_BASE_URL=http://localhost:8080
VITE_KEYCLOAK_URL=http://localhost:8180
VITE_KEYCLOAK_REALM=corporate
VITE_KEYCLOAK_CLIENT_ID=fp-auth-client
```

---

## 9. Useful Commands

```bash
# Stop all services
docker compose down

# Reset OpenLDAP (re-seeds bootstrap.ldif on next start)
docker compose rm -sf openldap && docker compose up -d openldap

# Reset Keycloak (realm config is preserved via volume)
docker compose restart keycloak

# Wipe Keycloak data completely and start fresh
docker compose rm -sf keycloak && docker volume rm fp-be_keycloak_data && docker compose up -d keycloak

# View backend logs
docker compose logs -f auth-db

# Run backend tests (no Docker needed — uses H2 + mocked services)
cd auth && mvn test

# Re-verify SES sender after LocalStack restart (required every time LocalStack container restarts)
aws --endpoint-url=http://localhost:4566 ses verify-email-identity \
  --email-address noreply@shop.com --region us-east-1
```

---

## 10. Production Checklist

- [ ] Set `JWT_SECRET` to a cryptographically random ≥ 32-byte value (Vault / KMS)
- [ ] Set `AD_LDAP_PASSWORD` from secrets manager
- [ ] Set `CORS_ALLOWED_ORIGINS` to exact production frontend URL
- [ ] Set `AWS_ENDPOINT` to blank (uses real AWS SQS/SES)
- [ ] Replace Keycloak with Azure AD — update `AD_JWKS_URI`, `AD_ISSUER`, `AD_AUDIENCE`
- [ ] Use LDAPS (`ldaps://`) for LDAP in production
- [ ] Disable SQL debug logging (`application-prod.properties`)
- [ ] Enable HTTPS at load balancer / ingress
