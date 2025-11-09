# Local Development Environment Setup

## Overview

This guide explains how to set up and configure the RAP (Raptor) application for local development using Docker Compose.

## Prerequisites

- Docker Desktop installed and running
- PowerShell (Windows) or Bash (Linux/Mac)
- Git with submodules initialized
- At least 8GB RAM available for Docker
- Ports available: 1433, 4200, 8080, 8090, 9090

## Quick Start

### 1. Initial Setup

```powershell
cd backend
.\dev.ps1 Setup
```

This command:
- Creates `.env` file from `.env.example`
- Sets up necessary configuration for local development
- Prepares Docker network

### 2. Start Services

**Option A: Start All Services (Full Stack)**
```powershell
.\dev.ps1 Dev-Full
```

Starts:
- Frontend (Angular) - http://localhost:4200
- Backend (Spring Boot) - http://localhost:8080
- Process Service (jBPM) - http://localhost:8090
- Keycloak (OIDC) - http://localhost:9090
- SQL Server Database - localhost:1433
- PostgreSQL (Keycloak DB) - internal only

**Option B: Start Backend Only**
```powershell
.\dev.ps1 Dev-Start
```

Starts:
- Backend (Spring Boot) - http://localhost:8080
- SQL Server Database - localhost:1433

### 3. View Logs

```powershell
# All services
.\dev.ps1 Logs

# Specific service
.\dev.ps1 Logs backend
.\dev.ps1 Logs keycloak
.\dev.ps1 Logs database
```

### 4. Stop Services

```powershell
.\dev.ps1 Dev-Stop
```

## Environment Configuration (.env file)

The `.env` file contains all configuration for local development. Here's what each property means:

### Azure Container Registry (ACR) Properties

These properties are required for pulling pre-built Docker images:

```bash
# Your ACR login server
ACR_LOGIN_SERVER=ngraptordev.azurecr.io

# Image versions to pull (use 'latest' for local dev)
FRONTEND_VERSION=latest
BACKEND_VERSION=latest
PROCESS_SERVICE_VERSION=latest
```

**Why 'latest'?**
- ✅ **Correct for local development** - Always pulls the most recent build
- ✅ Matches the development workflow where images are continuously built
- ✅ Ensures you have the newest features and fixes

**For Production:**
- Use specific version tags (e.g., `v1.0.0`, `prod-2024-11-06`)
- Never use `latest` in production deployments

### Database Configuration

```bash
# SQL Server credentials
DB_USERNAME=sa
DB_PASSWORD=YourStrong@Passw0rd
```

**Notes:**
- Password must meet SQL Server complexity requirements (uppercase, lowercase, numbers, symbols)
- Default `sa` user has full admin rights
- Database is automatically created via Flyway migrations

### Spring Security Configuration

```bash
# Basic auth credentials (legacy, replaced by OIDC)
SPRING_SECURITY_USER_NAME=admin
SPRING_SECURITY_USER_PASSWORD=admin123
```

**Notes:**
- These are for basic HTTP authentication
- When OIDC is configured, these are optional
- Useful for API testing without OIDC

### OIDC Authentication (Keycloak)

```bash
# Keycloak server URL
OIDC_PROVIDER_ISSUER_URI=http://localhost:9090/realms/raptor

# Client credentials (configured in Keycloak)
OIDC_CLIENT_ID=raptor-client
OIDC_CLIENT_SECRET=

# Frontend URL for CORS and OAuth redirects
FRONTEND_URL=http://localhost:4200
```

**How to get OIDC_CLIENT_SECRET:**
1. Start Keycloak: `.\dev.ps1 Dev-Full`
2. Wait ~60 seconds for Keycloak to start
3. Access: http://localhost:9090/admin (admin/admin)
4. Follow setup guide: `docs/KEYCLOAK-LOCAL-SETUP.md`
5. Create realm `raptor` and client `raptor-client`
6. Copy secret from Credentials tab
7. Paste into `.env` file

**Important:**
- Leave `OIDC_CLIENT_SECRET` empty until you've configured Keycloak
- Backend will start without OIDC if secret is missing
- Complete OIDC setup guide before testing authentication

### Keycloak Service Configuration

```bash
# Keycloak admin credentials
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin

# Keycloak's PostgreSQL database
KEYCLOAK_DB_USER=keycloak
KEYCLOAK_DB_PASSWORD=keycloak
```

**Access Keycloak:**
- URL: http://localhost:9090
- Admin Console: http://localhost:9090/admin
- Username: `admin`
- Password: `admin`

**Notes:**
- Keycloak uses PostgreSQL (not SQL Server) for its own data
- PostgreSQL database is internal to Docker network
- Keycloak admin credentials are separate from your application users

### Spring Profile

```bash
SPRING_PROFILES_ACTIVE=docker
```

**Profile Options:**
- `docker` - For Docker Compose environment (default)
- `dev` - For running backend directly (not in container)
- `prod` - For production deployment (Azure)

### Application Port

```bash
APP_PORT=8080
```

Backend service port (usually don't need to change).

## Port Assignments

| Service | Port | Access URL |
|---------|------|------------|
| Frontend | 4200 | http://localhost:4200 |
| Backend | 8080 | http://localhost:8080 |
| Process Service | 8090 | http://localhost:8090 |
| Keycloak | 9090 | http://localhost:9090 |
| SQL Server | 1433 | localhost:1433 |
| PostgreSQL | 5432 | Internal only |

## Docker Compose Profiles

Services are organized into profiles for selective startup:

### Default Profile (No flag needed)
- Backend
- SQL Server Database

```powershell
docker-compose up -d
# OR
.\dev.ps1 Dev-Start
```

### Full Stack Profile
- Frontend
- Backend
- Process Service
- Keycloak + PostgreSQL
- SQL Server Database

```powershell
docker-compose --profile full-stack up -d
# OR
.\dev.ps1 Dev-Full
```

## Common Tasks

### Login to Azure Container Registry

Before pulling images, authenticate to ACR:

```powershell
.\dev.ps1 ACR-Login
```

This requires:
- Azure CLI installed (`az`)
- Access to the ACR (ngraptordev.azurecr.io)

### Rebuild Backend Image

After code changes, rebuild the backend:

```powershell
# Stop services
.\dev.ps1 Dev-Stop

# Rebuild backend image
docker-compose build backend

# Start services
.\dev.ps1 Dev-Start
```

### Access Database

**Using Azure Data Studio or SQL Server Management Studio:**
- Server: `localhost,1433`
- Authentication: SQL Server Authentication
- Username: `sa`
- Password: `YourStrong@Passw0rd`
- Database: `raptordb`

**Using sqlcmd (in Docker):**
```powershell
docker exec -it rap-database /opt/mssql-tools18/bin/sqlcmd `
  -S localhost -U sa -P 'YourStrong@Passw0rd' -C
```

### Reset Database

```powershell
# Stop services
.\dev.ps1 Dev-Stop

# Remove database volume
docker volume rm backend_database-data

# Start services (database recreated with migrations)
.\dev.ps1 Dev-Start
```

### Reset Keycloak

```powershell
# Stop services
.\dev.ps1 Dev-Stop

# Remove Keycloak volume
docker volume rm backend_keycloak-db-data

# Start services (Keycloak DB recreated)
.\dev.ps1 Dev-Full

# Reconfigure Keycloak (see docs/KEYCLOAK-LOCAL-SETUP.md)
```

## Troubleshooting

### Issue: "invalid reference format"

**Cause:** Missing or empty environment variables in `.env`

**Fix:**
```powershell
# Regenerate .env file
.\dev.ps1 Setup

# OR manually check .env has:
# ACR_LOGIN_SERVER=ngraptordev.azurecr.io
# FRONTEND_VERSION=latest
# BACKEND_VERSION=latest
# PROCESS_SERVICE_VERSION=latest
```

### Issue: "Cannot pull image from ACR"

**Cause:** Not authenticated to Azure Container Registry

**Fix:**
```powershell
# Login to Azure
az login

# Login to ACR
.\dev.ps1 ACR-Login

# OR manually
az acr login --name ngraptordev
```

### Issue: "Port already in use"

**Cause:** Another service is using the port

**Fix:**
```powershell
# Find what's using the port (e.g., 8080)
netstat -ano | findstr :8080

# Stop the process using that port
# OR change the port in docker-compose.yml
```

### Issue: "Keycloak not starting"

**Cause:** Keycloak takes 60-90 seconds to start

**Fix:**
```powershell
# Check Keycloak logs
.\dev.ps1 Logs keycloak

# Wait for message: "Keycloak 23.0 started"
# Then access: http://localhost:9090
```

### Issue: "Backend can't connect to database"

**Cause:** Database not fully started or wrong credentials

**Fix:**
```powershell
# Check database health
docker ps

# Should show: rap-database (healthy)

# Check database logs
.\dev.ps1 Logs database

# Verify .env has correct DB_PASSWORD
```

### Issue: "Frontend not loading"

**Cause:** Frontend image might not exist or ACR authentication failed

**Fix:**
```powershell
# Pull frontend image manually
docker pull ngraptordev.azurecr.io/rap-frontend:latest

# If fails, ensure ACR login succeeded
.\dev.ps1 ACR-Login
```

## Environment Variables Reference

### Required Variables (Must Be Set)

| Variable | Default | Description |
|----------|---------|-------------|
| `ACR_LOGIN_SERVER` | `ngraptordev.azurecr.io` | Azure Container Registry URL |
| `DB_USERNAME` | `sa` | SQL Server username |
| `DB_PASSWORD` | `YourStrong@Passw0rd` | SQL Server password |

### Optional Variables (Have Defaults)

| Variable | Default | Description |
|----------|---------|-------------|
| `FRONTEND_VERSION` | `latest` | Frontend image tag |
| `BACKEND_VERSION` | `latest` | Backend image tag |
| `PROCESS_SERVICE_VERSION` | `latest` | Process service image tag |
| `SPRING_PROFILES_ACTIVE` | `docker` | Spring Boot profile |
| `KEYCLOAK_ADMIN` | `admin` | Keycloak admin username |
| `KEYCLOAK_ADMIN_PASSWORD` | `admin` | Keycloak admin password |
| `FRONTEND_URL` | `http://localhost:4200` | Frontend URL for CORS |

### OIDC Variables (Optional Until Keycloak Configured)

| Variable | Default | Description |
|----------|---------|-------------|
| `OIDC_PROVIDER_ISSUER_URI` | `http://localhost:9090/realms/raptor` | Keycloak realm URL |
| `OIDC_CLIENT_ID` | `raptor-client` | OAuth2 client ID |
| `OIDC_CLIENT_SECRET` | (empty) | OAuth2 client secret (from Keycloak) |

## Best Practices

### 1. Use 'latest' for Local Development
- ✅ Always get the newest builds
- ✅ Matches CI/CD workflow
- ✅ Easier to test new features

### 2. Don't Commit .env File
- `.env` is in `.gitignore`
- Contains local configuration
- Use `.env.example` as template

### 3. Keep ACR Credentials Secure
- ACR login uses Azure CLI (OAuth)
- No passwords stored in `.env`
- Credentials expire after 3 hours (re-run ACR-Login)

### 4. Start Services Incrementally
- Start with `Dev-Start` (backend + DB)
- Test backend APIs work
- Then try `Dev-Full` for complete stack

### 5. Monitor Logs
- Use `.\dev.ps1 Logs` regularly
- Check for errors during startup
- Look for "Started Application" message

## Next Steps

1. ✅ Configure `.env` file (this guide)
2. ⬜ Set up Keycloak OIDC provider → See `docs/KEYCLOAK-LOCAL-SETUP.md`
3. ⬜ Start all services → `.\dev.ps1 Dev-Full`
4. ⬜ Test authentication flow → See `frontend/FRONTEND-OIDC-IMPLEMENTATION.md`
5. ⬜ Deploy to Azure → See `infra/README.md`

## Related Documentation

- **Keycloak Setup**: `docs/KEYCLOAK-LOCAL-SETUP.md`
- **Docker Internals**: `DOCKER-INTERNALS.md`
- **Backend README**: `README.md`
- **Quick Start**: `QUICK-START.md`
- **Testing Guide**: `TESTING-GUIDE.md`

## Summary

Your `.env` file should look like this for local development:

```bash
# ACR Configuration
ACR_LOGIN_SERVER=ngraptordev.azurecr.io
FRONTEND_VERSION=latest      # ✅ Use 'latest' for local dev
BACKEND_VERSION=latest        # ✅ Use 'latest' for local dev
PROCESS_SERVICE_VERSION=latest # ✅ Use 'latest' for local dev

# Database
DB_USERNAME=sa
DB_PASSWORD=YourStrong@Passw0rd

# Spring Security
SPRING_SECURITY_USER_NAME=admin
SPRING_SECURITY_USER_PASSWORD=admin123

# OIDC (fill after Keycloak setup)
OIDC_PROVIDER_ISSUER_URI=http://localhost:9090/realms/raptor
OIDC_CLIENT_ID=raptor-client
OIDC_CLIENT_SECRET=<get-from-keycloak>
FRONTEND_URL=http://localhost:4200

# Keycloak
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin
KEYCLOAK_DB_USER=keycloak
KEYCLOAK_DB_PASSWORD=keycloak

# Spring
SPRING_PROFILES_ACTIVE=docker
APP_PORT=8080
```

**All properties are now documented and your existing values are preserved!** ✨
