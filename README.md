# RAP Backend Service

Spring Boot REST API service for the RAP (Rapid Application Platform) application.

## Quick Start

### Prerequisites
- Docker Desktop (Windows/Mac) or Docker Engine (Linux)
- Azure Container Registry (ACR) credentials
- **No additional tools required** - use PowerShell script (Windows) or Makefile (cross-platform)

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

| Task | PowerShell (Windows) | Make (Cross-platform) |
|------|---------------------|----------------------|
| Start backend + database | `.\dev.ps1 Dev-Start` | `make dev-start` |
| Rebuild backend | `.\dev.ps1 Dev-Rebuild` | `make dev-rebuild` |
| View logs | `.\dev.ps1 Dev-Logs` | `make dev-logs` |
| Stop services | `.\dev.ps1 Dev-Stop` | `make dev-stop` |
| Check status | `.\dev.ps1 Dev-Status` | `make dev-status` |
| Initialize database | `.\dev.ps1 DB-Init` | `make db-init` |
| Clean up | `.\dev.ps1 Clean` | `make clean` |
| Show all commands | `.\dev.ps1 Help` | `make help` |

## Documentation

### Getting Started
- **[Local Environment Setup](LOCAL-ENVIRONMENT-SETUP.md)** - ⭐ **START HERE** - Complete .env configuration guide
- **[Local Development Guide](LOCAL-DEVELOPMENT.md)** - Development workflow and best practices
- **[Keycloak Setup](../docs/KEYCLOAK-LOCAL-SETUP.md)** - OIDC authentication provider configuration

### Reference Guides
- **[Docker Internals](DOCKER-INTERNALS.md)** - Deep dive into Docker layers, caching, and optimization
- **[Accessing the App](ACCESSING-THE-APP.md)** - API authentication and endpoint details
- **[Environment Variables](ENVIRONMENT-VARIABLES.md)** - Environment configuration across dev/test/prod
- **[Docker README](README-DOCKER.md)** - Basic Docker usage guide

### Implementation Guides
- **[OIDC Implementation](docs/OIDC-IMPLEMENTATION-SUMMARY.md)** - Backend OIDC authentication details
- **[Testing Guide](TESTING-GUIDE.md)** - API testing and validation
- **[Quick Reference](QUICK-REFERENCE.md)** - Common commands and patterns

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
├── dev.ps1                    # PowerShell development script (Windows)
├── Makefile                   # Make development commands (cross-platform)
├── .env.example               # Environment template
└── pom.xml                    # Maven dependencies
```

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

See [Environment Variables Guide](ENVIRONMENT-VARIABLES.md) for complete details.

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

See [Accessing the App](ACCESSING-THE-APP.md) for detailed API documentation.

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
1. Check [LOCAL-DEVELOPMENT.md](LOCAL-DEVELOPMENT.md) troubleshooting section
2. Review logs: `make dev-logs`
3. Contact the development team
