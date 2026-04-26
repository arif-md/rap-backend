# Backend Application Configuration Architecture

## Overview

The backend uses a layered configuration strategy with four environment-specific Spring Boot profiles
(`dev`, `test`, `train`, `prod`). Each profile provides:

1. **Static OIDC/AAD constants** — hardcoded provider endpoints and client IDs in `application-{env}.properties`.
2. **Operational tunables** — JWT settings, CORS, frontend URL loaded from Azure App Configuration at startup.
3. **Secrets** — JWT signing key and AAD client secret resolved via App Config Key Vault references using
   the container's managed identity.

---

## Configuration Layers (lowest → highest priority)

| Layer | Source | What lives here |
|-------|--------|-----------------|
| 1 | `application.properties` | Defaults (App Config disabled, local SQL datasource) |
| 2 | `application-{profile}.properties` | OIDC/AAD provider endpoints + client IDs (environment constants) |
| 3 | Azure App Configuration (bootstrap) | JWT settings, CORS, frontend URL, KV references for secrets |
| 4 | Container App environment variables | `APP_CONFIG_ENDPOINT`, `AZURE_CLIENT_ID`, `SPRING_PROFILES_ACTIVE`, SQL connection string |

> App Configuration properties have higher priority than profile file properties.
> Properties in layer 3 override the same key in layer 2.

---

## Profile Activation

`SPRING_PROFILES_ACTIVE` is set to the Azure environment name (e.g. `dev`) by
`infra/app/backend-springboot.bicep` at deployment time via the `springProfile` parameter
(which receives `environmentName` from `main.bicep`).

This activates two profile-specific files:

- `bootstrap-{profile}.properties` — loaded during the **bootstrap phase** to connect to App Configuration
  before the main application context starts.
- `application-{profile}.properties` — loaded during the **main context phase** with OIDC/AAD constants.

---

## Profile Files

### `application-{env}.properties` (OIDC/AAD constants)

Each environment has a dedicated properties file with hardcoded OIDC/AAD values that don't change
at runtime and don't need refresh:

| File | Profile | OIDC Provider | Notes |
|------|---------|--------------|-------|
| `application-dev.properties` | `dev` | Login.gov INT sandbox (`idp.int.identitysandbox.gov`) | Dev/test environment |
| `application-test.properties` | `test` | Login.gov INT sandbox | Verify values with test team |
| `application-train.properties` | `train` | Login.gov INT sandbox | May use production Login.gov |
| `application-prod.properties` | `prod` | Login.gov PRODUCTION (`secure.login.gov`) | Update `REPLACE_WITH_PROD_*` placeholders |

All environments use AAD tenant `60a52712-6791-451d-bdec-5021c5f60a64` and AAD client
`8e2bc82f-76db-43ba-b07e-2dc5f4a5769f` — update `application-prod.properties` for production.

The `application-{env}.properties` files include `${AZURE_AD_CLIENT_SECRET:}` as a fallback with an
empty default. At runtime the App Config KV reference takes precedence, so the env var fallback is
only triggered if App Configuration is unreachable (in which case AAD login fails gracefully — no
startup crash).

### `bootstrap-{env}.properties` (App Config connection)

Each environment's bootstrap file configures the App Configuration store connection:

```properties
spring.cloud.azure.appconfiguration.stores[0].endpoint=${APP_CONFIG_ENDPOINT:}
spring.cloud.azure.appconfiguration.stores[0].selects[0].key-filter=app:
spring.cloud.azure.appconfiguration.stores[0].selects[0].label-filter={env}
spring.cloud.azure.appconfiguration.stores[0].monitoring.triggers[0].label={env}
spring.cloud.azure.credential.managed-identity-enabled=true
spring.cloud.azure.credential.client-id=${AZURE_CLIENT_ID:}
```

The `label-filter` must match the label applied to App Config entries by Bicep (which uses
`environmentName` as the label). The sentinel monitoring trigger also uses the same label.

---

## Azure App Configuration Entries

All entries use the `app:` key prefix (avoids slashes in ARM resource names). The library strips
the prefix before exposing properties, so `app:jwt.issuer` → Spring property `jwt.issuer`.

### Plain key-value entries (labeled `{environmentName}`)

| App Config key | Spring property | Source |
|----------------|-----------------|--------|
| `app:jwt.issuer` | `jwt.issuer` | `jwtIssuer` Bicep param |
| `app:jwt.access-token-expiration-minutes` | `jwt.access-token-expiration-minutes` | `jwtAccessTokenExpirationMinutes` param |
| `app:jwt.refresh-token-expiration-days` | `jwt.refresh-token-expiration-days` | `jwtRefreshTokenExpirationDays` param |
| `app:cors.allowed-origins` | `cors.allowed-origins` | Computed from frontend FQDN |
| `app:frontend.url` | `frontend.url` | Computed from frontend FQDN |
| `app:sentinel` | (monitoring trigger only) | Auto-updated by Bicep when entries change |

### Key Vault reference entries (labeled `{environmentName}`)

These use content-type `application/vnd.microsoft.appconfig.keyvaultref+json;charset=utf-8`.
The Spring Cloud Azure library resolves the KV URI using the container's managed identity at startup.

| App Config key | Spring property | KV secret | Java consumer |
|----------------|-----------------|-----------|---------------|
| `app:jwt.secret` | `jwt.secret` | `jwt-secret` | `JwtTokenUtil` — `@Value("${jwt.secret}")` (no default; crash if missing) |
| `app:spring.security.oauth2.client.registration.azure-ad.client-secret` | `spring.security.oauth2.client.registration.azure-ad.client-secret` | `aad-client-secret` | Spring Security OAuth2 AAD registration |

> **AAD entry is conditional** on `enableAadSso=true` (default `true`) passed to Bicep.
> Set `ENABLE_AAD_SSO=false` via `azd env set` to skip the AAD KV reference entry.

---

## Key Vault Secrets

Secrets are seeded by `infra/scripts/ensure-keyvault.sh` (pre-provision hook) using the `create-only`
pattern — existing secrets are never overwritten by deployment.

| KV Secret name | Workflow env var | Seeded by |
|----------------|-----------------|-----------|
| `oidc-client-secret` | `OIDC_CLIENT_SECRET` GitHub secret | `ensure-keyvault.sh` |
| `jwt-secret` | `JWT_SECRET` GitHub secret | `ensure-keyvault.sh` |
| `aad-client-secret` | `AZURE_AD_CLIENT_SECRET` GitHub secret | `ensure-keyvault.sh` |

These env vars remain in the GitHub Actions workflow `azd env set` steps **for KV seeding only**.
They are **not** Bicep parameters — `main.parameters.json` no longer references them.

To rotate a secret: update the KV secret value manually in the Azure Portal (or via `az keyvault secret set`),
then update the sentinel key in App Config to trigger a refresh in running containers within ~30 seconds.
No redeployment needed.

---

## Managed Identity Requirements

The backend user-assigned managed identity (`id-backend-{env}-*`) must have:

1. **App Configuration Data Reader** RBAC role on the App Config store (assigned by Bicep).
2. **Key Vault Secrets User** RBAC role (or access policy with `get` permission) on the Key Vault
   (set by `infra/scripts/ensure-identities.sh` pre-provision hook).

Both must be in place before the container starts. `ensure-identities.sh` runs before every `azd up`,
so timing is not an issue for normal deployments.

---

## Why No Container App SecretRefs?

Prior to this architecture, `jwt-secret` and `aad-client-secret` were declared as Container App
`secretRefs` pointing to Key Vault. This caused a race condition: ARM validates KV references at
deployment time, before managed identity data-plane permissions finish propagating. The result was
intermittent `unable to fetch secret` errors.

App Config KV references are resolved at Spring Boot startup time (~30–60 seconds after the Container
App revision is created). By this point, KV access policy propagation is complete — the race condition
is eliminated at the architectural level.

---

## Local Development

For local development, the `local` profile is used (`SPRING_PROFILES_ACTIVE=local` in VS Code tasks
and `dev.ps1`). The `application.properties` base file disables App Configuration, and the local
datasource points to the SQL Server container. No App Config or Key Vault access is needed locally.

OIDC/AAD config for local dev is injected directly as environment variables (see VS Code task
configuration in `.vscode/tasks.json`).
