# Keycloak — Azure AD Simulation for Local Dev

Keycloak runs at **http://localhost:8180** and simulates Azure AD for local development.
Everything below is a **one-time setup** — data persists as long as the container is not removed.

> **Important:** All steps must be done inside the **`corporate`** realm, NOT the default `master` realm.
> The realm selector is the dropdown in the top-left corner of the Admin UI.
> If it shows **"Keycloak"** you are in `master` — switch before doing anything.

---

## Prerequisites

```bash
# From fp-be/ root — start Keycloak (and all other services)
docker compose up -d
```

Wait ~10 seconds for Keycloak to be ready, then open **http://localhost:8180**.

---

## Step 1 — Log in to the Admin UI

- URL: **http://localhost:8180**
- Username: `admin`
- Password: `admin`

You will land on the **master** realm. Do not create anything here.

---

## Step 2 — Create the `corporate` realm

1. Click the realm dropdown in the **top-left** corner (shows "Keycloak" / "master")
2. Click **"Create realm"**
3. Set **Realm name** → `corporate`
4. Leave **Enabled** ON
5. Click **Create**

After saving, the top-left dropdown will show **`corporate`**. All remaining steps are inside this realm.

---

## Step 3 — Create the `fp-auth-client` OIDC client

1. In the left sidebar click **Clients** → **Create client**

**General Settings tab:**
- Client type: `OpenID Connect`
- Client ID: `fp-auth-client`
- Name: `fp-auth-client`
- Click **Next**

**Capability config tab:**
- Client authentication: **OFF** (public client)
- Authorization: **OFF**
- Authentication flow — check **only**:
  - [x] Standard flow
  - [x] Direct access grants  ← **required** for the test `curl` commands below
- Click **Next**

**Login settings tab:**
- Valid redirect URIs: `http://localhost:3000/*`  (add `http://localhost:4200/*` if needed)
- Web origins: `http://localhost:3000`
- Click **Save**

---

## Step 4 — Create test users

Repeat for each user below.

1. Left sidebar → **Users** → **Add user**
2. Fill in:
   - **Email verified**: ON
   - **Username** and **Email**: same value (see table)
   - **First name** / **Last name**: as shown
3. Click **Create**
4. Go to the **Credentials** tab → **Set password**
   - Set the password (see table)
   - **Temporary**: OFF
   - Click **Save password**

| Username / Email | First name | Last name | Password | LDAP group |
|---|---|---|---|---|
| `alice@corp.example.com` | Alice | Retail | `Alice@Pass1!` | GRP-RETAIL-CUSTOMERS |
| `bob@corp.example.com` | Bob | Staff | `Bob@Pass1!` | GRP-BANK-STAFF |
| `carol@corp.example.com` | Carol | Admin | `Carol@Pass1!` | GRP-SYSTEM-ADMINS |

> These passwords match the entries in `auth/docker/ldap/bootstrap.ldif`.

---

## Step 5 — Enable AD mode in `application.properties`

Add or update these properties in `auth/src/main/resources/application.properties`
(or export as environment variables):

```properties
# AD / OIDC — point to local Keycloak
app.ad.enabled=true
app.ad.jwks-uri=http://localhost:8180/realms/corporate/protocol/openid-connect/certs
app.ad.issuer=http://localhost:8180/realms/corporate
app.ad.audience=fp-auth-client
app.ad.unmapped-group-strategy=DEFAULT
app.ad.default-group-name=RETAIL_CUSTOMER

# LDAP — point to local OpenLDAP container
app.ad.ldap.url=ldap://localhost:389
app.ad.ldap.base=dc=corp,dc=example,dc=com
app.ad.ldap.user-dn=cn=svc-ldap,ou=service,dc=corp,dc=example,dc=com
app.ad.ldap.password=svc-password
app.ad.ldap.group-search-base=ou=groups,dc=corp,dc=example,dc=com
app.ad.ldap.group-search-filter=(member={0})
app.ad.ldap.group-id-attribute=cn
app.ad.ldap.group-name-attribute=cn
app.ad.ldap.user-search-base=ou=users,dc=corp,dc=example,dc=com
app.ad.ldap.user-search-filter=(mail={0})
```

---

## Step 6 — Verify the setup

**6a. Check the JWKS endpoint is reachable:**
```bash
curl -s http://localhost:8180/realms/corporate/protocol/openid-connect/certs | jq .
```
Expected: a JSON object with a `keys` array containing at least one RSA key.

**6b. Get a test ID token:**
```bash
curl -s -X POST http://localhost:8180/realms/corporate/protocol/openid-connect/token \
  -d 'grant_type=password' \
  -d 'client_id=fp-auth-client' \
  -d 'scope=openid' \
  -d 'username=alice@corp.example.com' \
  -d 'password=Alice@Pass1!' \
  | jq -r '.id_token'
```
Expected: a JWT string (three dot-separated base64 segments).
If you get `null` or an error, check that **Direct access grants** is enabled on the client (Step 3).

**6c. Exchange the ID token for a service JWT pair:**
```bash
ID_TOKEN=$(curl -s -X POST http://localhost:8180/realms/corporate/protocol/openid-connect/token \
  -d 'grant_type=password' \
  -d 'client_id=fp-auth-client' \
  -d 'scope=openid' \
  -d 'username=alice@corp.example.com' \
  -d 'password=Alice@Pass1!' | jq -r '.id_token')

curl -s -X POST http://localhost:8080/auth/ad/login \
  -H 'Content-Type: application/json' \
  -d "{\"idToken\": \"$ID_TOKEN\"}" | jq
```
Expected: `{ "status": "SUCCESS", "data": { "accessToken": "...", "refreshToken": "...", "user": { ... } } }`

---

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `id_token` is `null` | Direct access grants not enabled | Step 3 — enable "Direct access grants" on client |
| `401 AD authentication failed` | Wrong realm in `app.ad.issuer` / `jwks-uri` | Confirm URLs contain `/realms/corporate/` not `/realms/master/` |
| `401 AD authentication failed` | `fp-auth-client` created in `master` realm | Delete it, switch to `corporate` realm, redo Step 3 |
| `503 AD login is disabled` | `app.ad.enabled=false` | Set `app.ad.enabled=true` in properties |
| LDAP groups not synced | Wrong LDAP password | Confirm `app.ad.ldap.password=svc-password` |
| LDAP groups not synced | OpenLDAP not seeded | Run `docker compose rm -sf openldap && docker compose up -d openldap` |

---

## Resetting Keycloak

Keycloak stores data in memory (`KC_DB: dev-mem`) — the `corporate` realm is lost on container restart.
Re-run Steps 2–4 after every `docker compose down` / container removal.

```bash
# Quick reset — removes container so next start is clean
docker compose rm -sf keycloak
docker compose up -d keycloak
```

---

## Production (Azure AD)

Replace the Keycloak-specific properties with your Azure AD tenant values:

```properties
app.ad.enabled=true
app.ad.jwks-uri=https://login.microsoftonline.com/{tenantId}/discovery/v2.0/keys
app.ad.issuer=https://login.microsoftonline.com/{tenantId}/v2.0
app.ad.audience={applicationClientId}
# LDAP points to your corporate on-premise DC or Azure AD DS
app.ad.ldap.url=ldaps://corp-ldap.example.com:636
app.ad.ldap.password=${AD_LDAP_PASSWORD}   # store in Vault / Secrets Manager
```
