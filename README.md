# RAP Backend Service

Spring Boot REST API service for the RAP (Rapid Application Platform) application.

## Quick Start

### Prerequisites
- **For Docker Development:**
  - Docker Desktop (Windows/Mac) or Docker Engine (Linux)
  - Azure Container Registry (ACR) credentials
- **For Local Maven Development:**
  - Java 21 (JDK)
  - SQL Server container running (via `dev.ps1 Dev-Start`)
  - Keycloak container running (via `dev.ps1 Dev-Full`)

### Two Development Modes

#### Mode 1: Docker Containers (Full Stack)
**Best for:** Testing full application, running all services together

**Windows (PowerShell):**
```powershell
.\dev.ps1 Setup          # Create .env from template
notepad .env             # Edit with your ACR credentials
.\dev.ps1 ACR-Login      # Authenticate to ACR
.\dev.ps1 Dev-Full       # Start all services (frontend, backend, database, keycloak)
```

**Cross-platform (Make):**
```bash
make setup               # Create .env from template
nano .env                # Edit with your ACR credentials
make acr-login           # Authenticate to ACR
make dev-full            # Start all services
```

#### Mode 2: Local Maven (Backend Only)
**Best for:** Active backend development with hot reload

**Prerequisites:**
```powershell
# First start database and Keycloak containers
.\dev.ps1 Setup          # Create .env (one time)
.\dev.ps1 Dev-Start      # Start database container
# OR for Keycloak: .\dev.ps1 Dev-Full then stop backend container
```

**Run Backend Locally:**
```powershell
# Foreground mode (default) - hot reload enabled
.\run-local.ps1
# OR explicitly
.\run-local.ps1 Start

# Background mode - manual reload required
.\run-local.ps1 Background

# View logs (background mode only)
.\run-local.ps1 Logs

# Stop backend
.\run-local.ps1 Stop

# Show help
.\run-local.ps1 Help
```

**Hot Reload Behavior:**
- **Foreground mode** (`.\run-local.ps1`): 
  - ✅ Spring Boot DevTools auto-detects Java class changes
  - ✅ Application restarts automatically on save (3-5 seconds)
  - ✅ Best for active development
- **Background mode** (`.\run-local.ps1 -Background`):
  - ❌ Hot reload NOT available
  - Must stop and restart manually
- **VS Code Agent mode**:
  - Changes are staged but NOT deployed until you accept them
  - Prevents untested code from running

### Setup

1. **Clone and configure**
   
   **Windows (PowerShell):**
   ```powershell
   .\dev.ps1 Setup
   # Edit .env with your ACR credentials
   notepad .env
   ```
   
   **Cross-platform (Make):**
   ```bash
   make setup
   # Edit .env with your ACR credentials
   nano .env
   ```

2. **Login to ACR**
   
   **Windows:**
   ```powershell
   .\dev.ps1 ACR-Login
   ```
   
   **Make:**
   ```bash
   make acr-login
   ```

3. **Start development**
   
   **Windows:**
   ```powershell
   # Backend + Database only (recommended)
   .\dev.ps1 Dev-Start
   
   # OR Full stack (all services)
   .\dev.ps1 Dev-Full
   ```
   
   **Make:**
   ```bash
   # Backend + Database only (recommended)
   make dev-start
   
   # OR Full stack (all services)
   make dev-full
   ```

4. **Access the application**
   - Backend API: http://localhost:8080
   - Health check: http://localhost:8080/actuator/health
   - API endpoint: http://localhost:8080/api/

### Common Commands

#### Docker Container Commands (dev.ps1 / Makefile)

| Task | PowerShell (Windows) | Make (Cross-platform) |
|------|---------------------|----------------------|
| **Setup & Login** |
| Create .env file | `.\dev.ps1 Setup` | `make setup` |
| Login to ACR | `.\dev.ps1 ACR-Login` | `make acr-login` |
| **Service Management** |
| Start backend + database | `.\dev.ps1 Dev-Start` | `make dev-start` |
| Start all services (full stack) | `.\dev.ps1 Dev-Full` | `make dev-full` |
| Stop all services | `.\dev.ps1 Dev-Stop` | `make dev-stop` |
| Restart services | `.\dev.ps1 Dev-Restart` | `make dev-restart` |
| Rebuild backend | `.\dev.ps1 Dev-Rebuild` | `make dev-rebuild` |
| **Monitoring** |
| View logs (all) | `.\dev.ps1 Dev-Logs` | `make dev-logs` |
| View logs (specific service) | `.\dev.ps1 Dev-Logs backend` | `make dev-logs SERVICE=backend` |
| Check status | `.\dev.ps1 Dev-Status` | `make dev-status` |
| **Database** |
| Initialize database | `.\dev.ps1 DB-Init` | `make db-init` |
| Connect to database | `.\dev.ps1 DB-Connect` | `make db-connect` |
| **Cleanup** |
| Clean containers | `.\dev.ps1 Clean` | `make clean` |
| Show all commands | `.\dev.ps1 Help` | `make help` |

#### Local Maven Commands (run-local.ps1)

| Task | Command | Description |
|------|---------|-------------|
| **Run Backend** |
| Start in foreground (default) | `.\run-local.ps1` or `.\run-local.ps1 Start` | Hot reload enabled, see logs directly |
| Start in background | `.\run-local.ps1 Background` | Hidden process, logs to file |
| **Monitoring** |
| View logs | `.\run-local.ps1 Logs` | Show live logs (background mode) |
| Stop backend | `.\run-local.ps1 Stop` | Terminate running process |
| **Help** |
| Show help | `.\run-local.ps1 Help` | Display all commands and usage |

**Key Differences:**
- **`dev.ps1`**: Manages Docker containers (full stack or partial)
- **`run-local.ps1`**: Runs backend via Maven locally (requires Java 21, supports hot reload)

## Development Workflow Comparison

### Docker Development (`dev.ps1`)

**Pros:**
✅ Production-like environment (same containers as deployed)  
✅ All services available (frontend, backend, database, keycloak)  
✅ No local Java/Node.js installation needed  
✅ Consistent across all developers  

**Cons:**
❌ Slower build/restart cycles (Docker image rebuild)  
❌ No hot reload for code changes  
❌ Higher resource usage (multiple containers)  

**Best For:**
- Testing full application flow
- Frontend-backend integration testing
- Reproducing production issues
- Onboarding new developers (minimal setup)

**Typical Workflow:**
```powershell
.\dev.ps1 Setup          # One-time setup
.\dev.ps1 ACR-Login      # One-time ACR authentication
.\dev.ps1 Dev-Full       # Start all services
# Make code changes
.\dev.ps1 Dev-Rebuild    # Rebuild and restart backend container
.\dev.ps1 Dev-Logs       # View logs
.\dev.ps1 Dev-Stop       # Stop when done
```

### Local Maven Development (`run-local.ps1`)

**Pros:**
✅ **Hot reload enabled** - code changes auto-restart (3-5 seconds)  
✅ Faster development cycle  
✅ Direct IDE debugging support  
✅ Lower resource usage (only backend runs locally)  

**Cons:**
❌ Requires Java 21 installed locally  
❌ Still needs database/keycloak containers running  
❌ Slightly different environment than production  

**Best For:**
- Active backend development (Java code changes)
- Quick iteration and testing
- Debugging with IDE breakpoints
- Learning Spring Boot internals

**Typical Workflow:**
```powershell
# One-time: Start database and Keycloak containers
.\dev.ps1 Dev-Start      # Start database container
# OR .\dev.ps1 Dev-Full then stop backend container manually

# Run backend locally
.\run-local.ps1          # Start in foreground (hot reload)
# Make code changes - app auto-restarts on save!
# Press Ctrl+C when done
```

**Recommendation:**
- **New to project?** Start with `dev.ps1 Dev-Full` (full Docker stack)
- **Backend development?** Use `run-local.ps1` (hot reload for faster iteration)
- **Integration testing?** Use `dev.ps1 Dev-Full` (all services together)

## Documentation

### Getting Started
- **[Local Environment Setup](docs/LOCAL-ENVIRONMENT-SETUP.md)** - ⭐ **START HERE** - Complete .env configuration guide
- **[Local Development Guide](docs/LOCAL-DEVELOPMENT.md)** - Development workflow and best practices
- **[Keycloak Setup](../docs/KEYCLOAK-LOCAL-SETUP.md)** - OIDC authentication provider configuration

### Configuration Management
- **[Configuration Management Guide](../docs/CONFIGURATION-MANAGEMENT.md)** - ⚙️ **IMPORTANT** - How config flows through local/Docker/Azure environments
  - Understand JWT timeout configuration across environments
  - Spring Boot profile precedence rules
  - Frontend runtime-config.json generation
  - Common configuration pitfalls and solutions

### Reference Guides
- **[Docker Internals](docs/DOCKER-INTERNALS.md)** - Deep dive into Docker layers, caching, and optimization
- **[Accessing the App](docs/ACCESSING-THE-APP.md)** - API authentication and endpoint details
- **[Environment Variables](docs/ENVIRONMENT-VARIABLES.md)** - Environment configuration across dev/test/prod
- **[Docker README](docs/README-DOCKER.md)** - Basic Docker usage guide

### Implementation Guides
- **[OIDC Implementation](docs/OIDC-IMPLEMENTATION-SUMMARY.md)** - Backend OIDC authentication details
- **[Testing Guide](docs/TESTING-GUIDE.md)** - API testing and validation
- **[Quick Reference](docs/QUICK-REFERENCE.md)** - Common commands and patterns

## Architecture

The RAP application consists of 4 services:

```
Frontend (Angular)  →  Backend (Spring Boot)  ←  Process Service (jBPM)
                              ↓
                       Database (SQL Server)
```

For local development:
- **Backend**: Built locally from source (this repo)
- **Frontend & Process Service**: Pulled from Azure Container Registry
- **Database**: SQL Server 2022 (official Microsoft image)

## Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/x/y/z/backend/
│   │   │   ├── BackendApplication.java
│   │   │   ├── ServletInitializer.java
│   │   │   └── controller/
│   │   │       └── HealthController.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
├── init-scripts/              # Database initialization scripts
├── docker-compose.yml         # Multi-service orchestration
├── docker-compose.override.yml # Local dev overrides
├── Dockerfile                 # Multi-stage build
├── dev.ps1                    # PowerShell script for Docker containers
├── run-local.ps1              # PowerShell script for local Maven development
├── Makefile                   # Make development commands (cross-platform)
├── .env.example               # Environment template
└── pom.xml                    # Maven dependencies
```

### Development Scripts

#### `dev.ps1` - Docker Container Management
Manages Docker containers for local development. Supports both partial (backend + database) and full stack deployment.

**Common Commands:**
- `.\dev.ps1 Setup` - Initialize environment
- `.\dev.ps1 Dev-Start` - Start backend + database containers
- `.\dev.ps1 Dev-Full` - Start all services (frontend, backend, database, keycloak)
- `.\dev.ps1 Dev-Stop` - Stop all containers
- `.\dev.ps1 Help` - Show all available commands

#### `run-local.ps1` - Local Maven Development
Runs Spring Boot backend locally via Maven (not in Docker). Supports hot reload for faster development cycles.

**Features:**
- **Foreground mode (default)**: Hot reload enabled, interactive logs
- **Background mode**: Runs as hidden process, logs to file
- **Spring Boot DevTools**: Auto-detects code changes and restarts app
- **VS Code Agent compatible**: Staged changes only deploy when accepted

**Common Commands:**
- `.\run-local.ps1` or `.\run-local.ps1 Start` - Start in foreground (hot reload)
- `.\run-local.ps1 Background` - Start in background
- `.\run-local.ps1 Stop` - Stop backend
- `.\run-local.ps1 Logs` - View logs
- `.\run-local.ps1 Help` - Show detailed help

**When to Use:**
- Use `dev.ps1` when you need full stack or multiple services
- Use `run-local.ps1` when actively developing backend code (faster iteration)

## Technology Stack

- **Framework**: Spring Boot 3.5.5
- **Language**: Java 17
- **Build Tool**: Maven 3.9.11
- **Database**: Microsoft SQL Server 2022
- **ORM**: MyBatis 3.0.5 + Spring Data JPA
- **Security**: Spring Security
- **Monitoring**: Spring Boot Actuator
- **Containerization**: Docker with multi-stage builds
- **Automation**: PowerShell script (Windows) + Makefile (cross-platform)

## Development Workflows

### Backend-Only Development (Fast Iteration)

**Windows (PowerShell):**
```powershell
# Start backend + database
.\dev.ps1 Dev-Start

# Make code changes in your IDE

# Rebuild and restart
.\dev.ps1 Dev-Rebuild

# View logs
.\dev.ps1 Dev-Logs backend
```

**Cross-platform (Make):**
```bash
# Start backend + database
make dev-start

# Make code changes in your IDE

# Rebuild and restart
make dev-rebuild

# View logs
make dev-logs-backend
```

### Full Stack Testing

**Windows:**
```powershell
# Pull latest service images
.\dev.ps1 Image-Pull

# Start all services
.\dev.ps1 Dev-Full

# Access:
# - Frontend: http://localhost:4200
# - Backend: http://localhost:8080
# - Process Service: http://localhost:8090
```

**Make:**
```bash
# Pull latest service images
make image-pull

# Start all services
make dev-full

# Access:
# - Frontend: http://localhost:4200
# - Backend: http://localhost:8080
# - Process Service: http://localhost:8090
```

### Database Management

**Windows:**
```powershell
# Initialize database
.\dev.ps1 DB-Init

# Connect to database
.\dev.ps1 DB-Connect

# Reset database (deletes all data)
.\dev.ps1 DB-Reset
```

**Make:**
```bash
# Initialize database
make db-init

# Connect to database
make db-connect

# Reset database (deletes all data)
make db-reset
```

## Configuration

### Environment Variables

Key variables in `.env`:

```bash
# ACR Configuration
ACR_LOGIN_SERVER=yourregistry.azurecr.io
ACR_USERNAME=yourregistry
ACR_PASSWORD=your-password

# Service Versions
FRONTEND_VERSION=latest
BACKEND_VERSION=dev
PROCESS_SERVICE_VERSION=latest

# Database
DB_PASSWORD=YourStrong@Passw0rd

# Spring Security
SPRING_SECURITY_USER_NAME=admin
SPRING_SECURITY_USER_PASSWORD=admin123
```

See [Environment Variables Guide](docs/ENVIRONMENT-VARIABLES.md) for complete details.

## Building and Testing

### Local Build (without Docker)

**Windows:**
```powershell
# Build JAR
.\dev.ps1 Build

# Run tests
.\dev.ps1 Test
```

**Make:**
```bash
# Build JAR
make build

# Run tests
make test
```

**Manual (any platform):**
```bash
./mvnw clean package -DskipTests  # Build
./mvnw test                        # Test
```

### Docker Build

**Windows:**
```powershell
# Build image
.\dev.ps1 Image-Build

# Push to ACR (for team sharing)
.\dev.ps1 Image-Push
```

**Make:**
```bash
# Build image
make image-build

# Push to ACR (for team sharing)
make image-push
```

## API Endpoints

### Health Check
```bash
GET http://localhost:8080/actuator/health
```

### Basic API
```bash
GET http://localhost:8080/api/
Authorization: Basic admin:admin123
```

See [Accessing the App](docs/ACCESSING-THE-APP.md) for detailed API documentation.

## Troubleshooting

### Cannot pull images from ACR

**Windows:**
```powershell
# Login again
.\dev.ps1 ACR-Login

# Verify credentials
Get-Content .env
```

**Make:**
```bash
# Login again
make acr-login

# Verify credentials
cat .env
```

### Backend won't start

**Windows:**
```powershell
# Check logs
.\dev.ps1 Dev-Logs backend

# Check database is healthy
docker ps --filter "name=rap-database"

# Wait for database (can take 30-60s on first start)
```

**Make:**
```bash
# Check logs
make dev-logs-backend

# Check database is healthy
docker ps --filter "name=rap-database"

# Wait for database (can take 30-60s on first start)
```

### Port already in use

**Windows:**
```powershell
# Stop all services
.\dev.ps1 Dev-Stop

# Or change ports in docker-compose.yml
```

**Make:**
```bash
# Stop all services
make dev-stop

# Or change ports in docker-compose.yml
```

### Database connection issues

**Windows:**
```powershell
# Verify database is running
.\dev.ps1 Dev-Status

# Check password complexity (SQL Server requires strong passwords)
# Must be at least 8 characters with uppercase, lowercase, numbers, symbols
```

**Make:**
```bash
# Verify database is running
make dev-status

# Check password complexity (SQL Server requires strong passwords)
# Must be at least 8 characters with uppercase, lowercase, numbers, symbols
```

For more troubleshooting, see [Local Development Guide](LOCAL-DEVELOPMENT.md#troubleshooting).

## Deployment

This repository is for local development. For Azure deployment:
- Images are built via CI/CD (GitHub Actions)
- Pushed to Azure Container Registry
- Deployed to Azure Container Apps via Bicep templates
- See the infrastructure repository for deployment configuration

## Contributing

1. Create a feature branch
2. Make changes and test locally: `make dev-rebuild`
3. Run tests: `make test`
4. Build Docker image: `make image-build`
5. Commit and push
6. CI/CD will build and push to ACR

## Team Resources

- **Infrastructure Repository**: Contains Bicep templates for Azure deployment
- **Frontend Repository**: Angular web application
- **Process Service Repository**: jBPM workflow engine
- **Azure Portal**: Access Container Apps, SQL Database, and monitoring

## License

[Your License Here]

## Support

For issues or questions:
1. Check [LOCAL-DEVELOPMENT.md](docs/LOCAL-DEVELOPMENT.md) troubleshooting section
2. Review logs: `make dev-logs`
3. Contact the development team
