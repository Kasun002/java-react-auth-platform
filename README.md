# java-react-auth-platform

Enterprise authentication and RBAC platform — JWT auth service with Keycloak/LDAP SSO, role-based access control, OTP verification, and a React admin frontend.

---

## Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3, PostgreSQL, Redis, Flyway |
| Auth | JWT (HS512), Spring Security 6, Spring LDAP |
| SSO | Keycloak (dev) / Keycloak (prod), OIDC + PKCE |
| Async | AWS SQS + SES (LocalStack in dev) |
| Frontend | React 19, Vite, TailwindCSS, Axios |
| Infrastructure | Docker Compose, Kubernetes (k8s/) |

---

## Project Structure

```
fp-be/
├── auth-be/            # Spring Boot auth service (port 8080)
├── auth-fe/            # React admin frontend (port 5173)
├── k8s/                # Kubernetes manifests
├── docker-compose.yml  # Local infrastructure
└── README.md
```

---

## Prerequisites

- Java 21
- Maven 3.9+
- Node.js 20+
- Docker Desktop
- AWS CLI (for LocalStack SES setup)

---

## 1. Start Infrastructure

```bash
docker compose up -d
```

| Service | URL / Host | Credentials |
|---|---|---|
| PostgreSQL (auth) | `localhost:5433` | `admin / admin` |
| PostgreSQL (product) | `localhost:5434` | `admin / admin` |
| Redis | `localhost:6379` | — |
| pgAdmin | http://localhost:5050 | `admin@example.com / admin` |
| Keycloak | http://localhost:8180 | `admin / admin` |
| OpenLDAP | `localhost:389` | `cn=admin,dc=corp,dc=example,dc=com / admin` |
| phpLDAPadmin | http://localhost:6443 | same as OpenLDAP |

---

## 2. LocalStack Setup (one-time per container start)

LocalStack resets verified SES identities on every restart. After `docker compose up -d`:

```bash
aws --endpoint-url=http://localhost:4566 ses verify-email-identity \
  --email-address noreply@shop.com --region us-east-1
```

> OTP emails will fail with `MessageRejectedException` if this step is skipped or after a LocalStack container restart.

---

## 3. Keycloak Setup (one-time)

> Realm data persists across restarts via `dev-file` storage.

1. Open http://localhost:8180 → log in as `admin / admin`
2. **Create realm** → name: `corporate`
3. **Clients** → **Create client** → ID: `fp-auth-client` → enable **Direct access grants**
   - Valid redirect URIs: `http://localhost:5173/*`, `http://localhost:5174/*`
   - Web origins: `http://localhost:5173`
4. **Create test users** (Users → Add user → set email as username → Email verified ON → set password, Temporary OFF):

| Email | Password |
|---|---|
| `alice@corp.example.com` | `Alice@Pass1!` |
| `bob@corp.example.com` | `Bob@Pass1!` |
| `carol@corp.example.com` | `Carol@Pass1!` |

> `carol` is pre-seeded in `GRP-SYSTEM-ADMINS` (LDAP bootstrap) and maps to `SYSTEM_ADMIN` group via Flyway migration `V19`.

---

## 4. Start the Backend

```bash
cd auth
mvn spring-boot:run
```

| Endpoint | URL |
|---|---|
| API base | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |

---

## 5. Start the Frontend

```bash
cd auth-fe
npm install
npm run dev
```

App: http://localhost:5173

---

## 6. Granting Admin Access

Admin pages require RBAC permissions delivered through groups. The hierarchy is:

```
Users → Groups → Roles → Permissions
```

AD users' group memberships are managed in LDAP/Keycloak. Local users are assigned groups via the admin UI or API.

### AD user (Keycloak/LDAP)

`carol` is already in `GRP-SYSTEM-ADMINS` → maps to `SYSTEM_ADMIN` group → gets `ROLE_SYSTEM_ADMIN`. Log in via `POST /auth/ad/login` with a Keycloak `id_token`.

To add another user, add them to `GRP-SYSTEM-ADMINS` in OpenLDAP via phpLDAPadmin (http://localhost:6443) and create a matching Keycloak account.

### Local user

Assign the user to `SYSTEM_ADMIN` (or `SUPER_ADMIN`) via SQL:

```sql
INSERT INTO user_group_memberships (user_id, group_id)
SELECT u.id, g.id FROM users u
JOIN user_groups g ON g.name = 'SYSTEM_ADMIN'
WHERE u.email = 'youruser@example.com';
```

Or use the admin UI: **Users → assign group**.

### Group → permission mapping

| Group | Role | Key permissions |
|---|---|---|
| `SYSTEM_ADMIN` | `ROLE_SYSTEM_ADMIN` | `DASHBOARD_VIEW`, `USER_GROUPS_MANAGE`, `GROUP_MANAGE`, `ROLE_MANAGE`, `PERMISSION_MANAGE`, `AUDIT_LOG_VIEW`, `SYSTEM_CONFIG_VIEW` |
| `SUPER_ADMIN` | `ROLE_SUPER_ADMIN` | all permissions |
| `RETAIL_CUSTOMER` | `ROLE_CUSTOMER_BASIC` | `ACCOUNT_VIEW`, `TRANSACTION_VIEW` |

---

## 7. Environment Variables

Backend — sensible defaults for local dev; no `.env` file required.

| Variable | Default | Description |
|---|---|---|
| `JWT_SECRET` | (dev key) | Min 32-byte secret — **change in production** |
| `AD_ENABLED` | `true` | Enable AD/OIDC login |
| `AD_LDAP_PASSWORD` | `admin` | LDAP bind password (use `svc-ldap` in prod) |
| `CORS_ALLOWED_ORIGINS` | `localhost:5173,5174` | Frontend origins |
| `REDIS_HOST` | `localhost` | Redis host |
| `AWS_ENDPOINT` | `http://localhost:4566` | LocalStack endpoint (blank = real AWS) |

Frontend — `auth-fe/.env`:

```properties
VITE_API_BASE_URL=http://localhost:8080
VITE_KEYCLOAK_URL=http://localhost:8180
VITE_KEYCLOAK_REALM=corporate
VITE_KEYCLOAK_CLIENT_ID=fp-auth-client
VITE_AUDIT_PAGE_SIZE=10
```

---

## 8. Useful Commands

```bash
# Stop all services
docker compose down

# Reset OpenLDAP (re-seeds bootstrap.ldif on next start)
docker compose rm -sf openldap && docker compose up -d openldap

# Wipe Keycloak data and start fresh
docker compose rm -sf keycloak && docker volume rm fp-be_keycloak_data && docker compose up -d keycloak

# Re-verify SES sender after LocalStack restart
aws --endpoint-url=http://localhost:4566 ses verify-email-identity \
  --email-address noreply@shop.com --region us-east-1

# View backend logs
docker compose logs -f auth-db

# Run backend tests
cd auth && mvn test
```

---

## 9. Database Schema (ERD)

```mermaid
erDiagram
    USERS ||--o{ ADDRESSES : has
    USERS ||--o{ OTP_VERIFICATION : has
    USERS ||--o{ PASSWORD_HISTORY : has
    USERS ||--o{ USER_LOG : has
    USERS ||--o{ USER_ROLE_ASSIGNMENTS : has
    USERS ||--o{ USER_GROUP_MEMBERSHIPS : has
    USERS ||--o{ AUDIT_LOG : creates
    ROLES ||--o{ ROLE_PERMISSIONS : has
    ROLES ||--o{ GROUP_ROLES : has
    ROLES ||--o{ USER_ROLE_ASSIGNMENTS : assigned
    PERMISSIONS ||--o{ ROLE_PERMISSIONS : has
    USER_GROUPS ||--o{ GROUP_ROLES : has
    USER_GROUPS ||--o{ USER_GROUP_MEMBERSHIPS : has
    USER_GROUPS ||--o{ AD_GROUP_MAPPINGS : mapped

    USERS {
        int id PK
        string name
        string email UK
        string phone
        string password
        string status
        string date_of_birth
        string gender
        string profile_picture_url
        int failed_login_attempts
        string locked_until
        string last_login_at
        string password_changed_at
        string ad_object_id
        string auth_provider
        string created_at
        string updated_at
    }

    ADDRESSES {
        int id PK
        int user_id FK
        string address_line1
        string address_line2
        string street
        string postal_code
        string state
        string country
    }

    OTP_VERIFICATION {
        int id PK
        int user_id FK
        string otp_hash
        string expires_at
        int attempts
        int used
        string created_at
        string updated_at
    }

    PASSWORD_HISTORY {
        int id PK
        int user_id FK
        string password_hash
        string created_at
    }

    USER_LOG {
        int id PK
        int user_id FK
        string user_token
        string token_type
        string issued_at
        string expires_at
        string ip_address
        string user_agent
        string created_at
        string updated_at
    }

    ROLES {
        int id PK
        string name UK
        string description
        string created_at
        string updated_at
    }

    PERMISSIONS {
        int id PK
        string code UK
        string description
        string category
        string created_at
        string updated_at
    }

    ROLE_PERMISSIONS {
        int role_id PK, FK
        int permission_id PK, FK
    }

    USER_ROLE_ASSIGNMENTS {
        int user_id PK, FK
        int role_id PK, FK
        string assigned_at
    }

    USER_GROUPS {
        int id PK
        string name UK
        string description
        string type
        string created_at
        string updated_at
    }

    GROUP_ROLES {
        int group_id PK, FK
        int role_id PK, FK
    }

    USER_GROUP_MEMBERSHIPS {
        int user_id PK, FK
        int group_id PK, FK
        string assigned_at
    }

    AD_GROUP_MAPPINGS {
        int id PK
        string ad_group_id UK
        string ad_group_name
        int local_group_id FK
        int auto_created
        string created_at
        string updated_at
    }

    AUDIT_LOG {
        int id PK
        int actor_id FK
        string actor_name
        string action
        string resource
        string resource_id
        string details
        string ip_address
        string status
        string created_at
    }

    FLYWAY_SCHEMA_HISTORY {
        int installed_rank PK
        string version
        string description
        string type
        string script
        int checksum
        string installed_by
        string installed_on
        int execution_time
        int success
    }
```

---

## 10. Production Checklist

- [ ] Set `JWT_SECRET` to a cryptographically random ≥ 32-byte value (Vault / KMS)
- [ ] Set `AD_LDAP_USER_DN` / `AD_LDAP_PASSWORD` to the `svc-ldap` service account from secrets manager
- [ ] Set `CORS_ALLOWED_ORIGINS` to exact production frontend URL
- [ ] Set `AWS_ENDPOINT` to blank (uses real AWS SQS / SES)
- [ ] Replace Keycloak with Keycloak — update `AD_JWKS_URI`, `AD_ISSUER`, `AD_AUDIENCE`
- [ ] Use LDAPS (`ldaps://`) for LDAP
- [ ] Enable HTTPS at load balancer / ingress
- [ ] Disable SQL debug logging (`application-prod.properties`)
