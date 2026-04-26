# Azure Property Resolution — Two-Layer Config Model

This document explains how Spring Boot resolves configuration properties when the
backend runs in Azure with the `azure` profile active. The model has two distinct
layers: **Azure App Configuration** (loaded at bootstrap) and **application
property files** (loaded at startup).

## Table of Contents

- [Property Source Priority](#property-source-priority)
- [Layer 1: Bootstrap Phase (App Config)](#layer-1-bootstrap-phase-app-config)
- [Layer 2: Main Application Phase](#layer-2-main-application-phase)
- [How Resolution Works in Practice](#how-resolution-works-in-practice)
- [Placeholder Evaluation vs Direct Key Match](#placeholder-evaluation-vs-direct-key-match)
- [OIDC Additional Parameters (CustomAuthorizationRequestResolver)](#oidc-additional-parameters-customauthorizationrequestresolver)
- [Why Relaxed Binding Doesn't Apply to App Config Keys](#why-relaxed-binding-doesnt-apply-to-app-config-keys)
- [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Property Source Priority

When `SPRING_PROFILES_ACTIVE=azure`, Spring Boot's `Environment` contains property
sources in the following priority order (highest wins):

| Priority | Source | Example |
|----------|--------|---------|
| **1 (highest)** | OS environment variables / Container App env vars | `JWT_SECRET`, `APP_CONFIG_ENDPOINT`, `AZURE_CLIENT_ID` |
| **2** | Azure App Configuration (via Bootstrap) | `jwt.issuer`, `cors.allowed-origins`, `oidc.addl.req.param.acr.values` |
| **3** | Profile-specific properties file | `application-azure.properties` |
| **4 (lowest)** | Base properties file | `application.properties` |

> **Key rule:** When a property key exists at a higher-priority source, lower
> sources are never consulted for that key — even if the lower source has a
> `${PLACEHOLDER}` that could resolve to a different value.

---

## Layer 1: Bootstrap Phase (App Config)

### What happens

Before the main Spring Boot application context starts, the **Spring Cloud
Bootstrap** mechanism kicks in:

1. `spring-cloud-starter-bootstrap` activates `BootstrapConfiguration` from
   `spring.factories`.
2. `spring-cloud-azure-starter-appconfiguration-config` connects to Azure App
   Configuration using the endpoint in `bootstrap-azure.properties`.
3. It queries `app:*` keys with label `azure` (matching the active profile).
4. The `app:` prefix is stripped. For example, `app:jwt.issuer` becomes the
   Spring property `jwt.issuer`.
5. These properties are injected into the main application's `Environment` as a
   `BootstrapPropertySource`, which sits **above** `application.properties` /
   `application-azure.properties` but **below** OS environment variables.

### Key files

- **`bootstrap-azure.properties`** — configures the App Config connection:
  ```properties
  spring.cloud.azure.appconfiguration.enabled=true
  spring.cloud.azure.appconfiguration.stores[0].endpoint=${APP_CONFIG_ENDPOINT:}
  spring.cloud.azure.appconfiguration.stores[0].monitoring.enabled=true
  spring.cloud.azure.appconfiguration.stores[0].monitoring.triggers[0].key=app:sentinel
  ```

- **`infra/shared/app-configuration.bicep`** — source of truth for all App
  Config keys. Bicep manages the key–value entries using the `app:` prefix and
  `azure` label.

### What App Config stores (Bicep → App Config → Spring property)

| Bicep ARM resource name | App Config key | Spring property |
|-------------------------|----------------|-----------------|
| `app:jwt.issuer$azure` | `app:jwt.issuer` | `jwt.issuer` |
| `app:cors.allowed-origins$azure` | `app:cors.allowed-origins` | `cors.allowed-origins` |
| `app:oidc.addl.req.param.acr.values$azure` | `app:oidc.addl.req.param.acr.values` | `oidc.addl.req.param.acr.values` |
| `app:spring.security.oauth2.client.provider.oidc-provider.authorization-uri$azure` | `app:spring.security.oauth2.client.provider.oidc-provider.authorization-uri` | `spring.security.oauth2.client.provider.oidc-provider.authorization-uri` |

---

## Layer 2: Main Application Phase

After bootstrap completes, the main application context loads:

1. `application.properties` — base defaults for all profiles.
2. `application-azure.properties` — profile-specific overrides active when
   `SPRING_PROFILES_ACTIVE=azure`.
3. OS environment variables from the Container App definition.

These three sources merge with the already-loaded App Config properties to form
the complete `Environment`.

---

## How Resolution Works in Practice

### Scenario A: Property provided ONLY by App Config

**App Config has:** `jwt.issuer = raptor-app`

**`application.properties` has:**
```properties
jwt.issuer=${JWT_ISSUER:raptor-app}
```

**Container App env vars:** No `JWT_ISSUER` env var set.

**Resolution:**
1. Spring looks up `jwt.issuer` → finds it in BootstrapPropertySource (App Config) → **uses `raptor-app`**.
2. The `${JWT_ISSUER:raptor-app}` placeholder in `application.properties` is **never evaluated** because Spring found the value at a higher-priority source.

### Scenario B: OS env var overrides App Config

**App Config has:** `jwt.secret = some-value`

**Container App env vars:** `JWT_SECRET=azure-kv-secret-value`

**`application.properties` has:**
```properties
jwt.secret=${JWT_SECRET:default-dev-key}
```

**Resolution:**
1. Spring looks up `jwt.secret` → checks OS env vars first.
2. Spring's **relaxed binding** maps `jwt.secret` ↔ `JWT_SECRET` (dots → underscores, lowercase → uppercase).
3. Finds `JWT_SECRET` → **uses `azure-kv-secret-value`**.
4. Neither App Config nor `application.properties` is consulted.

### Scenario C: Property NOT in App Config, NOT in env vars

**Example:** `spring.datasource.hikari.maximum-pool-size`

**Resolution:**
1. Spring looks up the key → not in env vars, not in App Config.
2. Falls through to `application.properties` → finds `10`.

### Scenario D: Property in `application.properties` with placeholder, env var set

**`application.properties` has:**
```properties
spring.datasource.url=${AZURE_SQL_CONNECTIONSTRING:jdbc:sqlserver://localhost:1433;...}
```

**Container App env vars:** `AZURE_SQL_CONNECTIONSTRING=jdbc:sqlserver://sql-dev.database.windows.net:1433;...`

**Resolution:**
1. No higher source provides `spring.datasource.url` directly (not in App Config).
2. Spring reaches `application.properties`, finds the placeholder `${AZURE_SQL_CONNECTIONSTRING:...}`.
3. Resolves the placeholder: looks up `AZURE_SQL_CONNECTIONSTRING` in OS env vars → found → substitutes.
4. Result: `jdbc:sqlserver://sql-dev.database.windows.net:1433;...`

> **Critical distinction:** In Scenario A, the property KEY (`jwt.issuer`) was
> found directly at a higher-priority source, so the placeholder was never
> evaluated. In Scenario D, the property KEY (`spring.datasource.url`) was NOT
> in any higher source, so Spring falls through to `application.properties` and
> evaluates the placeholder.

---

## Placeholder Evaluation vs Direct Key Match

This is the most commonly misunderstood aspect. There are two fundamentally
different resolution paths:

### Path 1: Direct Key Match (App Config wins)

```
application.properties:
  jwt.issuer=${JWT_ISSUER:raptor-app}

App Config entry:
  jwt.issuer = raptor-app
```

Spring resolves `jwt.issuer` by iterating PropertySources from highest to lowest
priority. App Config's BootstrapPropertySource contains `jwt.issuer` → **returns
immediately**. The `${JWT_ISSUER:raptor-app}` placeholder in
`application.properties` is never reached and never evaluated.

**Even if** `JWT_ISSUER` were set as an env var with a different value, it would
make no difference — because `jwt.issuer` was found at the App Config level.
The only way `JWT_ISSUER` (the env var) would matter is if nothing above
`application.properties` provided `jwt.issuer` directly.

### Path 2: Placeholder Resolution (env var wins)

```
application.properties:
  spring.datasource.url=${AZURE_SQL_CONNECTIONSTRING:jdbc:sqlserver://localhost:1433}
```

No higher-priority source provides `spring.datasource.url` directly. Spring
reaches `application.properties`, sees the placeholder, and resolves
`${AZURE_SQL_CONNECTIONSTRING}` by looking it up (as a fresh lookup against all
PropertySources). It finds the OS env var → substitutes it.

### Why does this distinction matter?

If you set **both** an App Config key AND an env var for the same logical setting
using different key formats:

- App Config: `jwt.issuer = raptor-app` (dot-notation)
- Env var: `JWT_ISSUER = different-value`
- `application.properties`: `jwt.issuer=${JWT_ISSUER:default}`

The result is `raptor-app` (from App Config), **not** `different-value`. The env
var is ignored because Spring found `jwt.issuer` at the App Config level before
ever reaching the placeholder.

---

## OIDC Additional Parameters (CustomAuthorizationRequestResolver)

The OIDC additional request parameters (`acr_values`, `prompt`, `response_type`)
are **not** standard Spring Security properties — they are custom keys consumed
by `CustomAuthorizationRequestResolver` via `Environment.getProperty()`.

### Key format

The resolver builds dot-notation keys by replacing underscores with dots:

| OAuth2 param | Dot-notation key looked up | App Config stored key (with prefix) |
|--------------|----------------------------|--------------------------------------|
| `acr_values` | `oidc.addl.req.param.acr.values` | `app:oidc.addl.req.param.acr.values` |
| `prompt` | `oidc.addl.req.param.prompt` | `app:oidc.addl.req.param.prompt` |
| `response_type` | `oidc.addl.req.param.response.type` | `app:oidc.addl.req.param.response.type` |

### Why NOT environment variables?

These keys are **not** standard Spring Boot properties, so there is no
`${PLACEHOLDER}` in `application.properties` for them. The resolver calls
`environment.getProperty("oidc.addl.req.param.acr.values")` directly. This
means:

- In **Azure**: The value comes from App Config (loaded during bootstrap).
- In **local dev**: The value comes from environment variables — but only if
  Spring's relaxed binding maps the dot-notation key to the env var format. Since
  these are custom keys in a `BootstrapPropertySource`, **relaxed binding does
  NOT apply** (see below). For local dev, pass them as JVM system properties
  (`-Doidc.addl.req.param.acr.values=...`) or set them in a profile-specific
  properties file.

---

## Why Relaxed Binding Doesn't Apply to App Config Keys

Spring Boot's **relaxed binding** (e.g., `jwt.secret` ↔ `JWT_SECRET`) works
by delegating to `Binder`, which uses `ConfigurationPropertyName` to normalize
keys. This applies when:

1. Spring Boot resolves `@ConfigurationProperties` bindings.
2. Spring Boot resolves `@Value("${property.key}")` annotations.
3. Standard PropertySources (env vars, system properties, `.properties` files).

It does **NOT** apply to:

- Direct `environment.getProperty("some.key")` calls against a
  `BootstrapPropertySource`. The `getProperty` call performs an **exact string
  match** on the PropertySource's internal map. If App Config has
  `oidc.addl.req.param.acr.values`, calling
  `getProperty("oidc.addl.req.param.ACR.VALUES")` returns `null`.

This is why the `CustomAuthorizationRequestResolver` must use **exact
dot-notation keys** matching what Bicep wrote to App Config.

---

## Summary Cheat Sheet

```
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Environment                           │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Priority 1: OS Environment Variables                     │   │
│  │   JWT_SECRET=xxx, APP_CONFIG_ENDPOINT=https://...        │   │
│  │   (Set by Container App env vars in Bicep)               │   │
│  │   Relaxed binding: JWT_SECRET ↔ jwt.secret               │   │
│  └──────────────────────────────────────────────────────────┘   │
│                           │                                     │
│                    (if not found)                                │
│                           ▼                                     │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Priority 2: Azure App Config (BootstrapPropertySource)   │   │
│  │   jwt.issuer=raptor-app                                  │   │
│  │   cors.allowed-origins=https://...                       │   │
│  │   oidc.addl.req.param.acr.values=http://...              │   │
│  │   spring.security.oauth2.client.provider.oidc-provider.* │   │
│  │   (Managed by Bicep → app-configuration.bicep)           │   │
│  │   Exact key match only — no relaxed binding              │   │
│  └──────────────────────────────────────────────────────────┘   │
│                           │                                     │
│                    (if not found)                                │
│                           ▼                                     │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Priority 3: application-azure.properties                 │   │
│  │   server.forward-headers-strategy=native                 │   │
│  │   server.servlet.session.cookie.secure=true              │   │
│  │   spring.cloud.azure.appconfiguration.enabled=true       │   │
│  └──────────────────────────────────────────────────────────┘   │
│                           │                                     │
│                    (if not found)                                │
│                           ▼                                     │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Priority 4: application.properties                       │   │
│  │   jwt.secret=${JWT_SECRET:dev-default}                   │   │
│  │   spring.datasource.url=${AZURE_SQL_CONNECTIONSTRING:..} │   │
│  │   mybatis.mapper-locations=classpath:mapper/**/*.xml     │   │
│  │   (Placeholders evaluated HERE, not above)               │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Quick rules

1. **App Config provides property directly?** → Wins over `application.properties`.
   Placeholder in `application.properties` is never evaluated.

2. **Env var provides property directly?** → Wins over everything (App Config
   included) via relaxed binding.

3. **Neither provides the property?** → Falls through to `application.properties`.
   If it has a `${PLACEHOLDER}`, that placeholder is resolved against all sources.

4. **Custom keys via `getProperty()`** → Must be exact dot-notation match against
   App Config. No relaxed binding.

5. **Secrets** → Stay as Container App env vars (via Key Vault secretRef). NOT in
   App Config. Examples: `JWT_SECRET`, `AZURE_AD_CLIENT_SECRET`.

6. **Non-secret config** → Managed centrally in App Config via Bicep. Examples:
   OIDC endpoints, JWT timeouts, CORS origins, frontend URL.
