# Environment Variables Configuration

This document explains how environment variables are managed across different environments.

## Local Development (Docker)

For local Docker development, environment variables are loaded from the `.env` file.

### Setup

1. Copy the example file:
   ```bash
   cp .env.example .env
   ```

2. Edit `.env` with your local values:
   ```properties
   # Database Configuration
   DB_USERNAME=sa
   DB_PASSWORD=YourStrong@Passw0rd
   
   # Spring Security Configuration
   SPRING_SECURITY_USER_NAME=admin
   SPRING_SECURITY_USER_PASSWORD=admin123
   
   # Spring Profile
   SPRING_PROFILES_ACTIVE=docker
   ```

3. Run docker-compose (it automatically loads `.env`):
   ```bash
   docker-compose up -d
   ```

### Important Security Notes

- ✅ `.env` is in `.gitignore` - never committed to git
- ✅ `.env.example` is committed - shows required variables without secrets
- ✅ Use strong passwords for local development

## Azure Deployment

When deploying to Azure, environment variables are managed through Azure services and **NOT** from the `.env` file.

### Azure Container Apps

Set environment variables in the Container App configuration:

```bash
az containerapp update \
  --name my-app \
  --resource-group my-rg \
  --set-env-vars \
    SPRING_SECURITY_USER_NAME=admin \
    SPRING_SECURITY_USER_PASSWORD=secretref:app-password \
    SPRING_DATASOURCE_URL="jdbc:sqlserver://myserver.database.windows.net:1433;databaseName=mydb" \
    SPRING_DATASOURCE_USERNAME=sqladmin \
    SPRING_DATASOURCE_PASSWORD=secretref:db-password
```

### Azure App Service

Set environment variables through Azure Portal or CLI:

**Azure Portal:**
1. Go to your App Service
2. Settings → Configuration → Application settings
3. Add:
   - `SPRING_SECURITY_USER_NAME`
   - `SPRING_SECURITY_USER_PASSWORD`
   - `SPRING_DATASOURCE_URL`
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`

**Azure CLI:**
```bash
az webapp config appsettings set \
  --name my-app \
  --resource-group my-rg \
  --settings \
    SPRING_SECURITY_USER_NAME=admin \
    SPRING_SECURITY_USER_PASSWORD=@Microsoft.KeyVault(...) \
    SPRING_DATASOURCE_URL="jdbc:sqlserver://..." \
    SPRING_DATASOURCE_USERNAME=sqladmin \
    SPRING_DATASOURCE_PASSWORD=@Microsoft.KeyVault(...)
```

### Azure Kubernetes Service (AKS)

Use Kubernetes Secrets and ConfigMaps:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
type: Opaque
stringData:
  spring-security-password: "your-password"
  db-password: "your-db-password"
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
spec:
  template:
    spec:
      containers:
      - name: backend
        image: myregistry.azurecr.io/backend:latest
        env:
        - name: SPRING_SECURITY_USER_NAME
          value: "admin"
        - name: SPRING_SECURITY_USER_PASSWORD
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: spring-security-password
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: db-password
```

## Best Practices for Azure

### 1. Use Azure Key Vault for Secrets

Store sensitive values in Azure Key Vault and reference them:

```bash
# Store secrets in Key Vault
az keyvault secret set \
  --vault-name my-keyvault \
  --name app-password \
  --value "your-secure-password"

az keyvault secret set \
  --vault-name my-keyvault \
  --name db-password \
  --value "your-db-password"

# Reference in App Service
az webapp config appsettings set \
  --name my-app \
  --resource-group my-rg \
  --settings \
    SPRING_SECURITY_USER_PASSWORD=@Microsoft.KeyVault(SecretUri=https://my-keyvault.vault.azure.net/secrets/app-password/) \
    SPRING_DATASOURCE_PASSWORD=@Microsoft.KeyVault(SecretUri=https://my-keyvault.vault.azure.net/secrets/db-password/)
```

### 2. Use Managed Identity for Database Access

Instead of passwords, use Managed Identity for Azure SQL:

```bash
# Enable Managed Identity for your app
az webapp identity assign \
  --name my-app \
  --resource-group my-rg

# Grant access to Azure SQL
# (Configure in Azure SQL using Azure AD authentication)
```

Update connection string:
```properties
SPRING_DATASOURCE_URL=jdbc:sqlserver://myserver.database.windows.net:1433;databaseName=mydb;authentication=ActiveDirectoryMSI
```

### 3. Use Different Profiles per Environment

```properties
# Local (.env)
SPRING_PROFILES_ACTIVE=docker

# Azure Development
SPRING_PROFILES_ACTIVE=azure-dev

# Azure Production
SPRING_PROFILES_ACTIVE=azure-prod
```

Create corresponding `application-{profile}.properties` files.

## Environment Variable Hierarchy

The application reads environment variables in this order (last wins):

1. Default values in `application.properties`
2. Profile-specific properties (`application-docker.properties`, etc.)
3. Environment variables (from `.env` locally, Azure settings in cloud)
4. Command-line arguments

## Required Environment Variables

### Mandatory
- `SPRING_DATASOURCE_URL` - Database connection string
- `SPRING_DATASOURCE_USERNAME` - Database username
- `SPRING_DATASOURCE_PASSWORD` - Database password

### Optional (with defaults)
- `SPRING_SECURITY_USER_NAME` (default: `admin`)
- `SPRING_SECURITY_USER_PASSWORD` (default: `admin123`)
- `SPRING_PROFILES_ACTIVE` (default: `default`)
- `APP_PORT` (default: `8080`)

## Troubleshooting

### Check which environment variables are loaded

Add to `application.properties`:
```properties
logging.level.org.springframework.core.env=DEBUG
```

### Verify in running container

```bash
# Local Docker
docker exec -it spring-backend env | grep SPRING

# Azure Container Apps
az containerapp logs show \
  --name my-app \
  --resource-group my-rg
```

## Security Checklist

- [ ] `.env` file is in `.gitignore`
- [ ] Never commit actual secrets to git
- [ ] Use Azure Key Vault for production secrets
- [ ] Enable Managed Identity where possible
- [ ] Rotate passwords regularly
- [ ] Use strong, unique passwords (20+ characters)
- [ ] Restrict access to Azure Key Vault
- [ ] Enable Azure AD authentication for databases
- [ ] Use separate credentials per environment

## Example: Complete Azure Deployment

```bash
# 1. Create Azure resources
az group create --name my-rg --location eastus

az keyvault create \
  --name my-keyvault \
  --resource-group my-rg \
  --location eastus

# 2. Store secrets
az keyvault secret set \
  --vault-name my-keyvault \
  --name spring-security-password \
  --value "$(openssl rand -base64 32)"

# 3. Create Container App
az containerapp create \
  --name my-app \
  --resource-group my-rg \
  --environment my-env \
  --image myregistry.azurecr.io/backend:latest \
  --target-port 8080 \
  --ingress external \
  --env-vars \
    SPRING_PROFILES_ACTIVE=azure-prod \
    SPRING_SECURITY_USER_NAME=admin \
    SPRING_SECURITY_USER_PASSWORD=secretref:spring-security-password

# 4. Configure secrets
az containerapp secret set \
  --name my-app \
  --resource-group my-rg \
  --secrets spring-security-password=keyvaultref:https://my-keyvault.vault.azure.net/secrets/spring-security-password,identityref:/subscriptions/.../managedIdentities/my-identity
```

This ensures your application works seamlessly in both local Docker and Azure cloud environments!
