# Auth Service — Local Setup

## Prerequisites

- Java 21
- Maven 3.9+
- Docker & Docker Compose

---

## 1. Start infrastructure

From the repository root (`fp-be/`):

```bash
docker compose up -d
```

Services started:

| Service | Port | Credentials |
|---|---|---|
| PostgreSQL (auth_db) | 5433 | admin / admin |
| Redis | 6379 | — |
| pgAdmin | 5050 | admin@example.com / admin |
| Keycloak (AD simulation) | 8180 | admin / admin |
| OpenLDAP | 389 | cn=admin,dc=corp,dc=example,dc=com / admin |
| phpLDAPadmin | 6443 | — |

---

## 2. Configure application

`src/main/resources/application.properties` already has sensible defaults for local dev.

The only values you may need to override (via env vars or a local `application-local.properties`):

```properties
# Mail — optional; OTP emails go to SQS/SES; set if testing SMTP directly
MAIL_USERNAME=your@gmail.com
MAIL_PASSWORD=your-app-password
```

Everything else (JWT secret, DB, Redis, AWS LocalStack) works out of the box.

---

## 3. Run the application

```bash
cd auth
mvn spring-boot:run
```

API base URL: `http://localhost:8080`
Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## 4. Run tests

```bash
mvn test
```

Tests use H2 in-memory DB and mocked Redis/Mail — no Docker needed.

---

## 5. Azure AD login (optional)

Only needed if testing `POST /auth/ad/login`.

**One-time Keycloak setup** (after `docker compose up -d`):

1. Open `http://localhost:8180` → log in as `admin / admin`
2. Create realm: **`corporate`**
3. Create client: **`fp-auth-client`** (public, disable client auth)
4. Create test users: `alice@corp.example.com`, `bob@corp.example.com` — set permanent passwords

Enable AD in `application.properties`:

```properties
app.ad.enabled=true
app.ad.jwks-uri=http://localhost:8180/realms/corporate/protocol/openid-connect/certs
app.ad.issuer=http://localhost:8180/realms/corporate
app.ad.audience=fp-auth-client
app.ad.ldap.url=ldap://localhost:389
app.ad.ldap.password=svc-password
```

Get a token and test:

```bash
ID_TOKEN=$(curl -s -X POST http://localhost:8180/realms/corporate/protocol/openid-connect/token \
  -d 'grant_type=password' \
  -d 'client_id=fp-auth-client' \
  -d 'username=alice@corp.example.com' \
  -d 'password=Alice@Pass1!' | jq -r '.id_token')

curl -X POST http://localhost:8080/auth/ad/login \
  -H 'Content-Type: application/json' \
  -d "{\"idToken\": \"$ID_TOKEN\"}" | jq
```

---

## Quick reference

```bash
# Stop all services
docker compose down

# Reset OpenLDAP data (re-seeds bootstrap.ldif on next start)
docker compose rm -sf openldap && docker compose up -d openldap

# View logs
docker compose logs -f auth-db
```
