# Docker Setup for Spring Boot Backend

This guide explains how to build and run the Spring Boot application using Docker.

## Prerequisites

- Docker Desktop installed and running
- Docker Compose (usually included with Docker Desktop)

## Quick Start

### Option 1: Standard Build (Using Dockerfile)

```bash
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f backend

# Stop all services
docker-compose down

# Stop and remove volumes (cleans database)
docker-compose down -v
```

### Option 2: BuildKit Optimized Build (Recommended for Development)

```bash
# Enable BuildKit (already default in Docker Desktop 23.0+)
$env:DOCKER_BUILDKIT=1

# Build with BuildKit cache mounts (faster, more efficient)
docker build -f Dockerfile.buildkit -t backend:latest .

# Or use docker-compose with BuildKit
docker-compose build
docker-compose up -d
```

### 2. Build and Run with Docker Only

```bash
# Build the image
docker build -t backend:latest .

# Run the container
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL="jdbc:sqlserver://host.docker.internal:1433;databaseName=appdb" \
  -e SPRING_DATASOURCE_USERNAME=sa \
  -e SPRING_DATASOURCE_PASSWORD=YourPassword \
  backend:latest
```

## Configuration

### Environment Variables

Copy `.env.example` to `.env` and update with your values:

```bash
cp .env.example .env
```

Key environment variables:

- `DB_USERNAME`: Database username (default: sa)
- `DB_PASSWORD`: Database password
- `SPRING_PROFILES_ACTIVE`: Spring profile to activate
- `APP_PORT`: Application port (default: 8080)

### Database Connection

The application connects to SQL Server. When using docker-compose, the database is automatically created and configured.

Connection details:
- **Host**: sqlserver (internal) or localhost:1433 (external)
- **Database**: appdb
- **Username**: sa
- **Password**: Set via `DB_PASSWORD` environment variable

## Multi-Stage Build Explained

The Dockerfile uses a multi-stage build for optimization:

1. **Builder Stage**: Uses Maven Wrapper to compile and package the application
   - Caches dependencies for faster rebuilds
   - Produces the WAR file
   - Extracts layers for optimal Docker layer caching

2. **Runtime Stage**: Uses a lightweight JRE image
   - Only includes the compiled application
   - Runs as non-root user for security
   - Configures health checks and JVM options

### Dependency Caching Strategy

**Standard Dockerfile (`Dockerfile`):**
- Uses Docker layer caching
- Downloads dependencies only when `pom.xml` changes
- Dependencies stored in layer cache on host machine
- Good for CI/CD pipelines

**BuildKit Optimized (`Dockerfile.buildkit`):**
- Uses BuildKit cache mounts (persistent volumes)
- Maven dependencies persist across ALL builds
- Even faster rebuilds - dependencies never re-downloaded
- Best for local development
- Requires BuildKit enabled (default in modern Docker)

```dockerfile
# BuildKit cache mount - creates persistent /root/.m2 cache
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw dependency:go-offline -B
```

## Best Practices Implemented

### Security
- ✅ Non-root user execution
- ✅ No hardcoded credentials
- ✅ Minimal runtime image
- ✅ Health checks enabled

### Performance
- ✅ Layer caching for dependencies
- ✅ JVM optimized for containers
- ✅ G1GC garbage collector
- ✅ Connection pooling ready

### Reliability
- ✅ Health check configuration
- ✅ Proper signal handling
- ✅ Automatic restarts
- ✅ Graceful shutdown

## Useful Commands

### View Application Logs
```bash
docker-compose logs -f backend
```

### Access SQL Server
```bash
# Connect to SQL Server (note: -C flag required for SQL Server 2022)
docker exec -it sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P 'YourStrong@Passw0rd' -C

# Once connected, you can run SQL queries:
# 1> SELECT name FROM sys.databases;
# 2> GO
# 3> EXIT
```

### Rebuild After Code Changes
```bash
docker-compose up -d --build
```

### Check Container Health
```bash
docker inspect --format='{{json .State.Health}}' spring-backend
```

### Clean Up Everything
```bash
docker-compose down -v --rmi all
```

## Troubleshooting

### Container won't start
- Check logs: `docker-compose logs backend`
- Verify database is running: `docker-compose ps`
- Ensure ports 8080 and 1433 are not in use

### Database connection issues
- Verify SQL Server is healthy: `docker-compose ps sqlserver`
- Check connection string in environment variables
- Ensure database user has proper permissions

### Out of memory errors
- Adjust `JAVA_OPTS` in docker-compose.yml
- Increase Docker Desktop memory allocation

## Production Considerations

Before deploying to production:

1. Use secrets management (Azure Key Vault, Docker Secrets)
2. Enable SSL/TLS for database connections
3. Configure proper logging and monitoring
4. Set up backup strategies for database volumes
5. Use specific image tags instead of `latest`
6. Implement proper CI/CD pipelines
7. Configure resource limits in docker-compose.yml

## Azure Deployment

To deploy to Azure Container Apps or Azure Kubernetes Service:

1. Push image to Azure Container Registry
2. Configure managed identity for secure database access
3. Use Azure SQL Database instead of containerized SQL Server
4. Enable Application Insights for monitoring
5. Configure auto-scaling policies

See Azure deployment documentation for detailed steps.
