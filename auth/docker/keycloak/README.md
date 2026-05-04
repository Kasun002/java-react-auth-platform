# Keycloak — Azure AD Simulation for Local Dev

Keycloak runs at **http://localhost:8180** and simulates Azure AD for local development.

## First-time setup

### 1. Start Keycloak
```bash
docker compose up -d keycloak
```

### 2. Open the Admin UI
Navigate to http://localhost:8180 and log in with `admin` / `admin`.

### 3. Create the `corporate` realm
1. Click the realm dropdown (top-left) → **Create realm**
2. Name: `corporate`
3. Save

### 4. Create the `fp-auth-client` OIDC client
1. **Clients** → **Create client**
2. Client ID: `fp-auth-client`
3. Client authentication: **OFF** (public client for PKCE)
4. Valid redirect URIs: `http://localhost:3000/*`, `http://localhost:4200/*`
5. Web origins: `http://localhost:3000`, `http://localhost:4200`
6. Save

### 5. Create test users
1. **Users** → **Add user**
2. Create: `alice@corp.example.com`, `bob@corp.example.com`, `carol@corp.example.com`
3. Set passwords under the **Credentials** tab (disable "Temporary")

### 6. Verify the JWKS endpoint
```
GET http://localhost:8180/realms/corporate/protocol/openid-connect/certs
```

### 7. Get an ID token (for testing)
```bash
curl -s -X POST http://localhost:8180/realms/corporate/protocol/openid-connect/token \
  -d 'grant_type=password' \
  -d 'client_id=fp-auth-client' \
  -d 'username=alice@corp.example.com' \
  -d 'password=Alice@Pass1!' \
  | jq -r '.id_token'
```

### 8. Call the AD login endpoint
```bash
ID_TOKEN=$(curl -s -X POST http://localhost:8180/realms/corporate/protocol/openid-connect/token \
  -d 'grant_type=password' \
  -d 'client_id=fp-auth-client' \
  -d 'username=alice@corp.example.com' \
  -d 'password=Alice@Pass1!' | jq -r '.id_token')

curl -s -X POST http://localhost:8080/auth/ad/login \
  -H 'Content-Type: application/json' \
  -d "{\"idToken\": \"$ID_TOKEN\"}" | jq
```

## application.properties for local AD dev

```properties
app.ad.enabled=true
app.ad.jwks-uri=http://localhost:8180/realms/corporate/protocol/openid-connect/certs
app.ad.issuer=http://localhost:8180/realms/corporate
app.ad.audience=fp-auth-client
app.ad.unmapped-group-strategy=DEFAULT
app.ad.default-group-name=RETAIL_CUSTOMER

# LDAP (OpenLDAP container)
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

## Azure AD (production)

Replace the above with:

```properties
app.ad.enabled=true
app.ad.jwks-uri=https://login.microsoftonline.com/{tenantId}/discovery/v2.0/keys
app.ad.issuer=https://login.microsoftonline.com/{tenantId}/v2.0
app.ad.audience={yourClientId}
# LDAP stays the same but points to your corporate AD DS or Azure AD DS
```
