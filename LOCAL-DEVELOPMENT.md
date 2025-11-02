# Local Development Guide - RAP Backend Service

This guide covers running the complete RAP application stack locally using Docker Compose, including the Angular frontend, Spring Boot backend, jBPM process service, and SQL Server database.

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Development Workflows](#development-workflows)
- [Service Configuration](#service-configuration)
- [Database Management](#database-management)
- [Troubleshooting](#troubleshooting)
- [Advanced Topics](#advanced-topics)

---

## Architecture Overview

The RAP application consists of 4 main services:

```
┌─────────────────┐
│   Frontend      │  Angular Application
│   Port: 4200    │  (Pulled from ACR)
└────────┬────────┘
         │
         ├─────────────┐
         │             │
┌────────▼────────┐   ┌▼───────────────────┐
│   Backend       │   │  Process Service   │
│   Port: 8080    │◄──┤  Port: 8090        │
│   (Built Local) │   │  (Pulled from ACR) │
└────────┬────────┘   └┬───────────────────┘
         │             │
         └──────┬──────┘
                │
       ┌────────▼────────┐
       │   Database      │  SQL Server 2022
       │   Port: 1433    │  (Public Image)
       └─────────────────┘
```

### Service Details

| Service | Description | Port | Source |
|---------|-------------|------|--------|
| **frontend** | Angular web application | 4200 | ACR (pre-built image) |
| **backend** | Spring Boot REST API | 8080 | **Built locally** |
| **process-service** | jBPM workflow engine | 8090 | ACR (pre-built image) |
| **database** | Microsoft SQL Server | 1433 | Public (mcr.microsoft.com) |

### Development Strategy

- **Backend**: Built locally from source (this repository) for active development
- **Frontend & Process Service**: Pulled as pre-built images from Azure Container Registry
- **Database**: Uses official SQL Server 2022 image from Microsoft

---

## Prerequisites

### Required Software

1. **Docker Desktop** (Windows/Mac) or **Docker Engine** (Linux)
   - Version 20.10 or higher
   - Download: https://www.docker.com/products/docker-desktop

2. **Git**
   - For cloning the repository

3. **PowerShell** (Windows) or **Make** (cross-platform)
   - **Windows**: PowerShell comes pre-installed - use `dev.ps1` script (no extra tools needed!)
   - **Mac/Linux with Make**: Pre-installed on Mac with Xcode Command Line Tools, `sudo apt-get install make` on Linux
   - **Windows with Make** (optional): Install via Chocolatey: `choco install make`

### Azure Container Registry Access

You need credentials to pull pre-built images for frontend and process-service:

1. **ACR Login Server**: Your registry URL (e.g., `yourregistry.azurecr.io`)
2. **ACR Username**: Registry username
3. **ACR Password**: Registry password

Get credentials with Azure CLI:
```bash
az acr credential show --name yourregistry
```

---

## Quick Start

### 1. Initial Setup

**Windows (PowerShell - Recommended):**
```powershell
# Clone the repository
cd c:\tmp\source-code\rap-prototype\backend

# Create .env file from template
.\dev.ps1 Setup
# OR manually:
Copy-Item .env.example .env

# Edit .env file with your ACR credentials
notepad .env
```

**Cross-platform (Make):**
```bash
# Clone the repository
cd c:\tmp\source-code\rap-prototype\backend

# Create .env file from template
make setup
# OR manually:
cp .env.example .env

# Edit .env file with your ACR credentials
nano .env
```

### 2. Configure .env File

Edit `.env` and set your ACR credentials:

```bash
# Azure Container Registry Configuration
ACR_LOGIN_SERVER=yourregistry.azurecr.io
ACR_USERNAME=yourregistry
ACR_PASSWORD=your-acr-password-here

# Service versions
FRONTEND_VERSION=latest
BACKEND_VERSION=dev
PROCESS_SERVICE_VERSION=latest

# Database credentials
DB_PASSWORD=YourStrong@Passw0rd

# Spring Security
SPRING_SECURITY_USER_NAME=admin
SPRING_SECURITY_USER_PASSWORD=admin123
```

### 3. Login to Azure Container Registry

**Windows:**
```powershell
.\dev.ps1 ACR-Login
```

**Make:**
```bash
make acr-login
```

### 4. Start Services

**Option A: Backend + Database Only** (recommended for backend development)

**Windows:**
```powershell
.\dev.ps1 Dev-Start
```

**Make:**
```bash
make dev-start
```

**Option B: Full Stack** (all 4 services)

**Windows:**
```powershell
.\dev.ps1 Dev-Full
```

**Make:**
```bash
make dev-full
```

### 5. Verify Services

**Windows:**
```powershell
# Check container status
.\dev.ps1 Dev-Status

# View logs
.\dev.ps1 Dev-Logs
```

**Make:**
```bash
# Check container status
make dev-status

# View logs
make dev-logs
```

### 6. Access Applications

- **Frontend**: http://localhost:4200 (if running full stack)
- **Backend API**: http://localhost:8080
  - Health: http://localhost:8080/actuator/health
  - API: http://localhost:8080/api/
- **Process Service**: http://localhost:8090 (if running full stack)
- **Database**: `localhost:1433` (use SQL Server Management Studio or Azure Data Studio)

---

## Development Workflows

### Backend Development (Primary Workflow)

When actively developing the backend service:

**Windows:**
```powershell
# 1. Start backend + database
.\dev.ps1 Dev-Start

# 2. Make code changes in your IDE

# 3. Rebuild and restart backend
.\dev.ps1 Dev-Rebuild

# 4. View backend logs
.\dev.ps1 Dev-Logs backend
```

**Make:**
```bash
# 1. Start backend + database
make dev-start

# 2. Make code changes in your IDE

# 3. Rebuild and restart backend
make dev-rebuild

# 4. View backend logs
make dev-logs-backend
```

### Full Stack Development

When you need to test integration with frontend and process service:

```bash
# 1. Pull latest images from ACR
make image-pull

# 2. Start all services
make dev-full

# 3. View all logs
make dev-logs

# 4. Stop all services when done
make dev-stop
```

### Working with Different Service Versions

Edit `.env` to use specific versions:

```bash
# Use specific tagged versions
FRONTEND_VERSION=v1.2.3
BACKEND_VERSION=dev
PROCESS_SERVICE_VERSION=staging

# Use latest builds
FRONTEND_VERSION=latest
PROCESS_SERVICE_VERSION=latest
```

Then restart:
```bash
make dev-stop
make dev-full
```

---

## Service Configuration

### Docker Compose Files

The project uses multiple compose files:

1. **docker-compose.yml** - Base configuration for all services
2. **docker-compose.override.yml** - Local development overrides

Docker Compose automatically merges these files. The override file:
- Uses profiles to control which services start
- Enables debug logging for backend
- Configures local build settings

### Service Profiles

Services use Docker Compose profiles:

- **No profile** (default): Backend + Database only
- **full-stack** profile: All 4 services

```bash
# Start only backend + database
docker-compose up -d

# Start all services
docker-compose --profile full-stack up -d
```

### Environment Variables

All services use environment variables from `.env`:

| Variable | Description | Default |
|----------|-------------|---------|
| `ACR_LOGIN_SERVER` | Your ACR registry URL | `yourregistry.azurecr.io` |
| `ACR_USERNAME` | ACR username | `yourregistry` |
| `ACR_PASSWORD` | ACR password | (none) |
| `FRONTEND_VERSION` | Frontend image tag | `latest` |
| `BACKEND_VERSION` | Backend image tag | `dev` |
| `PROCESS_SERVICE_VERSION` | Process service image tag | `latest` |
| `DB_PASSWORD` | SQL Server SA password | `YourStrong@Passw0rd` |
| `SPRING_SECURITY_USER_NAME` | API username | `admin` |
| `SPRING_SECURITY_USER_PASSWORD` | API password | `admin123` |

---

## Database Management

### Initialize Database

Run initialization scripts:

```bash
make db-init
```

This executes SQL scripts from `init-scripts/` directory.

### Connect to Database

```bash
# Via Docker container
make db-connect

# Via external tools
# Host: localhost
# Port: 1433
# Username: sa
# Password: (from DB_PASSWORD in .env)
```

### Database Schema

The database contains:
- **appdb**: Backend application database
- **processdb**: jBPM process engine database

### Reset Database

**⚠️ WARNING: This deletes all data!**

```bash
make db-reset
```

This will:
1. Stop the database container
2. Delete the volume (all data lost)
3. Start a fresh database
4. You need to run `make db-init` after

---

## Troubleshooting

### Common Issues

#### 1. "Cannot pull image from ACR"

**Error**: `unauthorized: authentication required`

**Solution**:
```bash
# Login to ACR
make acr-login

# Verify credentials in .env
notepad .env
```

#### 2. "Backend container keeps restarting"

**Check**:
```bash
# View backend logs
make dev-logs-backend

# Check if database is healthy
docker ps --filter "name=rap-database"
```

**Common causes**:
- Database not ready yet (wait 30-60 seconds)
- Wrong database connection string
- Missing environment variables

#### 3. "Database healthcheck failing"

**Check**:
```bash
# View database logs
docker-compose logs database

# Verify password
echo $DB_PASSWORD
```

**Solution**:
- Ensure `DB_PASSWORD` meets SQL Server requirements (min 8 chars, complexity)
- Wait for SQL Server to fully start (can take 30-60 seconds)

#### 4. "Port already in use"

**Error**: `Bind for 0.0.0.0:8080 failed: port is already allocated`

**Solution**:
```bash
# Stop existing containers
make dev-stop

# Or change ports in docker-compose.yml
# ports:
#   - "8081:8080"  # Use 8081 instead
```

#### 5. "Frontend/Process Service not starting"

**Check** if you're using the correct profile:
```bash
# Full stack requires profile flag
make dev-full
# OR
docker-compose --profile full-stack up -d
```

### View Service Status

```bash
# Container status
make dev-status

# Detailed health checks
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# View logs for specific service
docker-compose logs <service-name>
# Examples:
docker-compose logs backend
docker-compose logs database
docker-compose logs frontend
```

### Clean Start

If you encounter persistent issues:

```bash
# Stop everything
make clean

# Rebuild from scratch
make dev-rebuild

# Nuclear option (deletes all data)
make clean-all
make dev-start
make db-init
```

---

## Advanced Topics

### Building Without Makefile

If you don't have `make` installed:

```powershell
# Start backend + database
docker-compose up -d backend database

# Start all services
docker-compose --profile full-stack up -d

# Rebuild backend
docker-compose up -d --build backend

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Clean up volumes
docker-compose down -v
```

### Live Reload for Backend

To enable live reload (requires spring-boot-devtools):

1. Add devtools dependency to `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

2. Uncomment volume mount in `docker-compose.override.yml`:
```yaml
backend:
  volumes:
    - ./target/classes:/app/classes
```

3. Build and run:
```bash
# Terminal 1: Build continuously
./mvnw spring-boot:run

# Terminal 2: Run containers
make dev-start
```

### Running Backend Outside Docker

For faster iteration during development:

```bash
# Start only database
docker-compose up -d database

# Run backend locally
./mvnw spring-boot:run

# Configure application.properties for localhost:
# spring.datasource.url=jdbc:sqlserver://localhost:1433;...
```

### Custom Network Configuration

Services communicate via Docker network `rap-network`:

```bash
# Inspect network
docker network inspect rap-network

# Services can reach each other by container name:
# - http://backend:8080
# - http://database:1433
# - http://frontend:80
# - http://process-service:8090
```

### Pushing Your Local Build to ACR

After building locally, push to ACR for team sharing:

```bash
# Build with ACR tag
make image-build

# Push to registry
make image-push

# Team members can now pull your build
make image-pull
```

---

## Useful Commands Reference

### Command Comparison

| Task | PowerShell (Windows) | Make (Cross-platform) |
|------|---------------------|----------------------|
| **Setup** | | |
| Create .env file | `.\dev.ps1 Setup` | `make setup` |
| Login to ACR | `.\dev.ps1 ACR-Login` | `make acr-login` |
| **Development** | | |
| Start backend + DB | `.\dev.ps1 Dev-Start` | `make dev-start` |
| Start all services | `.\dev.ps1 Dev-Full` | `make dev-full` |
| Stop all | `.\dev.ps1 Dev-Stop` | `make dev-stop` |
| Restart all | `.\dev.ps1 Dev-Restart` | `make dev-restart` |
| Rebuild backend | `.\dev.ps1 Dev-Rebuild` | `make dev-rebuild` |
| View logs (all) | `.\dev.ps1 Dev-Logs` | `make dev-logs` |
| View logs (service) | `.\dev.ps1 Dev-Logs backend` | `make dev-logs-backend` |
| Container status | `.\dev.ps1 Dev-Status` | `make dev-status` |
| **Database** | | |
| Run init scripts | `.\dev.ps1 DB-Init` | `make db-init` |
| Connect via sqlcmd | `.\dev.ps1 DB-Connect` | `make db-connect` |
| Reset database | `.\dev.ps1 DB-Reset` | `make db-reset` |
| **Cleanup** | | |
| Remove containers | `.\dev.ps1 Clean` | `make clean` |
| Remove all + data | `.\dev.ps1 Clean-All` | `make clean-all` |
| **Build** | | |
| Build JAR locally | `.\dev.ps1 Build` | `make build` |
| Run tests | `.\dev.ps1 Test` | `make test` |
| Build Docker image | `.\dev.ps1 Image-Build` | `make image-build` |
| Push to ACR | `.\dev.ps1 Image-Push` | `make image-push` |
| Pull from ACR | `.\dev.ps1 Image-Pull` | `make image-pull` |
| **Help** | | |
| Show all commands | `.\dev.ps1 Help` | `make help` |

### PowerShell Script Commands

For Windows users, the `dev.ps1` script provides all functionality without requiring Make:

```powershell
# Setup
.\dev.ps1 Setup              # Create .env file
.\dev.ps1 ACR-Login          # Login to ACR

# Development
.\dev.ps1 Dev-Start          # Backend + Database
.\dev.ps1 Dev-Full           # All services
.\dev.ps1 Dev-Stop           # Stop all
.\dev.ps1 Dev-Restart        # Restart all
.\dev.ps1 Dev-Rebuild        # Rebuild backend
.\dev.ps1 Dev-Logs           # View all logs
.\dev.ps1 Dev-Logs backend   # View backend logs only
.\dev.ps1 Dev-Status         # Container status

# Database
.\dev.ps1 DB-Init            # Run init scripts
.\dev.ps1 DB-Connect         # Connect with sqlcmd
.\dev.ps1 DB-Reset           # Reset database

# Cleanup
.\dev.ps1 Clean              # Remove containers
.\dev.ps1 Clean-All          # Remove everything + data

# Build
.\dev.ps1 Build              # Build JAR locally
.\dev.ps1 Test               # Run tests
.\dev.ps1 Image-Build        # Build Docker image
.\dev.ps1 Image-Push         # Push to ACR
.\dev.ps1 Image-Pull         # Pull from ACR

# Help
.\dev.ps1 Help               # Show all available commands
```

### Makefile Commands

For cross-platform development (or if you prefer Make on Windows):

```bash
# Setup
make setup              # Create .env file
make acr-login          # Login to ACR

# Development
make dev-start          # Backend + Database
make dev-full           # All services
make dev-stop           # Stop all
make dev-restart        # Restart all
make dev-rebuild        # Rebuild backend
make dev-logs           # View all logs
make dev-status         # Container status

# Database
make db-init            # Run init scripts
make db-connect         # Connect with sqlcmd
make db-reset           # Reset database

# Cleanup
make clean              # Remove containers
make clean-all          # Remove everything + data

# Build
make build              # Build JAR locally
make test               # Run tests
make image-build        # Build Docker image
make image-push         # Push to ACR
make image-pull         # Pull from ACR

# Help
make help               # Show all available commands
```

### Docker Compose Commands

```bash
# Start services
docker-compose up -d
docker-compose --profile full-stack up -d

# Stop services
docker-compose down
docker-compose down -v  # Include volumes

# View logs
docker-compose logs -f
docker-compose logs -f backend

# Status
docker-compose ps

# Rebuild
docker-compose up -d --build backend

# Execute commands in containers
docker-compose exec backend bash
docker-compose exec database /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa
```

---

## Additional Resources

- [Docker Documentation](DOCKER-INTERNALS.md) - Deep dive into Docker layers and caching
- [Accessing the App](ACCESSING-THE-APP.md) - API authentication and endpoints
- [Environment Variables](ENVIRONMENT-VARIABLES.md) - Env var management across environments
- [Docker README](README-DOCKER.md) - Basic Docker usage guide

---

## Getting Help

If you encounter issues:

1. Check this guide's [Troubleshooting](#troubleshooting) section
2. Review logs: `make dev-logs`
3. Verify configuration: `make dev-status`
4. Check ACR connectivity: `make acr-login`
5. Try clean restart: `make clean && make dev-start`

For team support, include:
- Output of `make dev-status`
- Relevant logs from `make dev-logs`
- Your `.env` file (without passwords!)
- Steps to reproduce the issue
