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

## 2. Keycloak Setup (one-time)

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

## 3. Start the Auth Service

```bash
cd auth
mvn spring-boot:run
```

API base URL: http://localhost:8080
Swagger UI: http://localhost:8080/swagger-ui.html

> AD login is enabled by default in `application.properties`. No extra config needed for local dev.

---

## 4. Start the Frontend

```bash
cd auth-fe
npm install
npm run dev
```

App URL: http://localhost:5173

---

## 5. Verify the Full Flow

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

## 6. Environment Variables

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

## 7. Useful Commands

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
```

---

## 8. Production Checklist

- [ ] Set `JWT_SECRET` to a cryptographically random ≥ 32-byte value (Vault / KMS)
- [ ] Set `AD_LDAP_PASSWORD` from secrets manager
- [ ] Set `CORS_ALLOWED_ORIGINS` to exact production frontend URL
- [ ] Set `AWS_ENDPOINT` to blank (uses real AWS SQS/SES)
- [ ] Replace Keycloak with Azure AD — update `AD_JWKS_URI`, `AD_ISSUER`, `AD_AUDIENCE`
- [ ] Use LDAPS (`ldaps://`) for LDAP in production
- [ ] Disable SQL debug logging (`application-prod.properties`)
- [ ] Enable HTTPS at load balancer / ingress
