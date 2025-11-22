# OIDC Custom Authorization Request Parameters

This document explains how to configure additional parameters that are sent with OAuth2/OIDC authorization requests to your custom OIDC provider.

## Overview

Some OIDC providers require additional parameters in the authorization request beyond the standard OAuth2/OIDC parameters. This application supports configuring these parameters via environment variables without code changes.

## How It Works

1. **Environment Variables**: Set variables with prefix `OIDC_ADDL_REQ_PARAM_` (e.g., `OIDC_ADDL_REQ_PARAM_ACR_VALUES`)
2. **Parameter Conversion**: The underscore in the variable name becomes the parameter name (e.g., `acr_values`)
3. **Authorization Request**: Parameters are automatically added to the OAuth2 authorization URL
4. **Default Behavior**: If no `OIDC_ADDL_REQ_PARAM_*` variables are set, no extra parameters are added (works with standard OIDC providers like Keycloak)

## Local Development Configuration

### Example: Keycloak (No Additional Parameters)

For local Keycloak, no additional configuration is needed. The `.env` file doesn't need any `OIDC_ADDL_REQ_PARAM_*` variables.

### Example: Custom OIDC Provider Requiring ACR Values

If your OIDC provider requires Authentication Context Class Reference (ACR) values:

**backend/.env:**
```bash
# Standard OIDC configuration
OIDC_AUTHORIZATION_ENDPOINT=https://your-provider.com/oauth2/authorize
OIDC_TOKEN_ENDPOINT=https://your-provider.com/oauth2/token
OIDC_USER_INFO_ENDPOINT=https://your-provider.com/oauth2/userinfo
OIDC_JWK_SET_URI=https://your-provider.com/oauth2/certs
OIDC_CLIENT_ID=your-client-id

# Additional authorization request parameters
# Spring Boot normalizes: OIDC_ADDL_REQ_PARAM_ACR_VALUES → oidc.addl.req.param.acr_values
OIDC_ADDL_REQ_PARAM_ACR_VALUES=http://idmanagement.dev/ns/assurance/ial/2
OIDC_ADDL_REQ_PARAM_PROMPT=login
OIDC_ADDL_REQ_PARAM_UI_LOCALES=en
```

This will add the following to the authorization request:
```
?acr_values=http://idmanagement.dev/ns/assurance/ial/2
&prompt=login
&ui_locales=en
```

## Azure Deployment Configuration

### Option 1: Using azd Environment Variables

```powershell
cd infra

# Set individual OIDC additional parameters
azd env set OIDC_ADDL_REQ_PARAM_ACR_VALUES "http://idmanagement.dev/ns/assurance/ial/2"
azd env set OIDC_ADDL_REQ_PARAM_PROMPT "login"
azd env set OIDC_ADDL_REQ_PARAM_RESPONSE_TYPE "code"

# Deploy
azd up
```

### Option 2: Using GitHub Actions

**GitHub Repository Variables** (Settings → Secrets and variables → Actions → Variables):

Add individual variables for each parameter you need:

- **Name**: `OIDC_ADDL_REQ_PARAM_ACR_VALUES`
  - **Value**: `http://idmanagement.dev/ns/assurance/ial/2`

- **Name**: `OIDC_ADDL_REQ_PARAM_PROMPT`
  - **Value**: `login`

- **Name**: `OIDC_ADDL_REQ_PARAM_RESPONSE_TYPE`
  - **Value**: `code`

The workflow will automatically set these in the azd environment, and they'll be passed to the backend container as environment variables.

## Supported Parameter Names

Common OIDC authorization request parameters:

| Parameter | Description | Example Value |
|-----------|-------------|---------------|
| `acr_values` | Authentication Context Class Reference | `http://idmanagement.dev/ns/assurance/ial/2` |
| `prompt` | Controls user interaction | `login`, `consent`, `select_account`, `none` |
| `ui_locales` | Preferred UI language | `en`, `es`, `fr` |
| `login_hint` | Pre-fill username/email | `user@example.com` |
| `display` | Display mode | `page`, `popup`, `touch`, `wap` |
| `max_age` | Maximum authentication age (seconds) | `3600` |
| `claims` | Specific claims requested (JSON) | See OIDC spec |
| `id_token_hint` | ID token from previous auth | (token string) |

**Note**: Parameter names use underscores (`acr_values`), which get converted from environment variable format (`ACR_VALUES`).

## Verification

### Check Backend Container Environment Variables (Azure)

```powershell
az containerapp show \
  --name dev-rap-be \
  --resource-group rg-raptor-test \
  --query "properties.template.containers[0].env[?contains(name, 'OIDC_ADDL')]" \
  -o table
```

### Check Authorization Request (Browser)

1. Open browser developer tools (F12)
2. Go to Network tab
3. Navigate to your application
4. Click login
5. Find the redirect to your OIDC provider
6. Check the URL parameters - you should see your custom parameters

Example:
```
https://your-provider.com/oauth2/authorize
  ?client_id=your-client
  &redirect_uri=https://your-backend/login/oauth2/code/oidc-provider
  &response_type=code
  &scope=openid%20profile%20email
  &acr_values=http://idmanagement.dev/ns/assurance/ial/2  ← Your custom parameter
  &prompt=login                                            ← Your custom parameter
  &code_challenge=...
  &code_challenge_method=S256
```

## Troubleshooting

### Parameters Not Appearing

1. **Check environment variables are set**:
   ```powershell
   # Local
   Get-Content .env | Select-String "OIDC_ADDL"
   
   # Azure
   az containerapp show --name dev-rap-be --resource-group rg-raptor-test \
     --query "properties.template.containers[0].env" -o json
   ```

2. **Verify Spring Boot property binding**:
   - Spring Boot converts `OIDC_ADDL_REQ_PARAM_ACR_VALUES` to `oidc.addl.req.param.acr_values`
   - Check backend logs for property loading

3. **Restart backend container** after changing environment variables:
   ```powershell
   # Local
   cd backend
   .\dev.ps1 Dev-Stop
   .\dev.ps1 Dev-Full
   
   # Azure - redeploy
   cd infra
   azd up
   ```

### OIDC Provider Rejects Request

- Verify your OIDC provider actually requires these parameters
- Check parameter values match provider documentation
- Review provider error messages for specific parameter issues
- Test with standard OIDC parameters first, then add custom ones

## Implementation Details

### Code Structure

1. **`CustomAuthorizationRequestResolver.java`**:
   - Loads `OIDC_ADDL_REQ_PARAM_*` environment variables on startup
   - Wraps Spring Security's default resolver
   - Adds configured parameters to every authorization request

2. **`backend-springboot.bicep`**:
   - Accepts individual string parameters: `oidcAcrValues`, `oidcPrompt`, `oidcResponseType`
   - Converts to environment variables with `OIDC_ADDL_REQ_PARAM_` prefix
   - Example: `oidcAcrValues: "val"` → `OIDC_ADDL_REQ_PARAM_ACR_VALUES=val`

3. **`main.parameters.json`**:
   - Maps azd environment variables to Bicep parameters
   - Example: `${OIDC_ADDL_REQ_PARAM_ACR_VALUES=}` → `oidcAcrValues`
   - Defaults to empty string if not set

4. **`provision-infrastructure.yaml` (GitHub Actions)**:
   - Reads GitHub repository variables: `OIDC_ADDL_REQ_PARAM_*`
   - Sets them in azd environment with same names
   - Bicep reads from azd environment

### Why This Design?

- **No code changes needed**: Add parameters via configuration only
- **Environment-specific**: Different parameters for local vs Azure
- **Backwards compatible**: Works with standard OIDC providers (Keycloak) without any configuration
- **Provider-agnostic**: Supports any OIDC provider's custom requirements
- **Simple and maintainable**: No JSON parsing, just individual string variables

## Examples by OIDC Provider

### Login.dev (Identity Sandbox)

```bash
# backend/.env
OIDC_ADDL_REQ_PARAM_ACR_VALUES=http://idmanagement.dev/ns/assurance/ial/2
```

### Azure AD B2C

```bash
# backend/.env
OIDC_ADDL_REQ_PARAM_P=B2C_1_SignUpSignIn
OIDC_ADDL_REQ_PARAM_PROMPT=login
```

### Okta

```bash
# backend/.env
OIDC_ADDL_REQ_PARAM_IDP=0oa123abc456
OIDC_ADDL_REQ_PARAM_PROMPT=login
```

### Auth0

```bash
# backend/.env
OIDC_ADDL_REQ_PARAM_CONNECTION=google-oauth2
OIDC_ADDL_REQ_PARAM_PROMPT=login
```

## References

- [OAuth 2.0 RFC 6749](https://datatracker.ietf.org/doc/html/rfc6749)
- [OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0.html#AuthRequest)
- [Spring Security OAuth2 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/advanced.html)
