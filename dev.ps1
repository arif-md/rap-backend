# Show help for all commands
function Help {
    Write-Host "RAP Backend Development Scripts" -ForegroundColor Cyan
    Write-Host "" 
    Write-Host "Usage:" -ForegroundColor White
    Write-Host "  ./dev.ps1 <Command> [Options]" -ForegroundColor White
    Write-Host "" 
    Write-Host "Commands:" -ForegroundColor Yellow
    Write-Host "  Run                 Run backend locally with hot reload (mvnw spring-boot:run)" -ForegroundColor White
    Write-Host "  Support-Start       Start supporting services only (DB, Keycloak, Processes)" -ForegroundColor White
    Write-Host "  Dev-Start           Start backend and database services" -ForegroundColor White
    Write-Host "  Dev-Full            Start all services (full stack)" -ForegroundColor White
    Write-Host "  Dev-Stop            Stop all services" -ForegroundColor White
    Write-Host "  Dev-Restart         Restart all services" -ForegroundColor White
    Write-Host "  Container-Stop      Pause just the backend container (no rebuild on resume)" -ForegroundColor White
    Write-Host "  Container-Start     Resume a container paused with Container-Stop" -ForegroundColor White
    Write-Host "  Dev-Rebuild [-ForceUpdate]  Rebuild backend and restart" -ForegroundColor White
    Write-Host "  Dev-Logs [service]  View logs for a service or all" -ForegroundColor White
    Write-Host "  Image-Build [-NoCache]  Build backend Docker image (use -NoCache to force full rebuild)" -ForegroundColor White
    Write-Host "  Clean-Maven-Cache   Wipe the BuildKit Maven (.m2) cache mount (corrupt/stale cache only)" -ForegroundColor White
    Write-Host "  Image-Push          Push backend image to ACR" -ForegroundColor White
    Write-Host "  DB-Init             Initialize database" -ForegroundColor White
    Write-Host "  DB-Connect          Connect to database" -ForegroundColor White
    Write-Host "  DB-Reset            Reset database (DANGEROUS)" -ForegroundColor White
    Write-Host "  Flyway-Repair       Fix 'checksum mismatch' errors after editing a migration file" -ForegroundColor White
    Write-Host "  Clean               Clean up containers and networks" -ForegroundColor White
    Write-Host "  Clean-All           Remove all containers, networks, and volumes" -ForegroundColor White
    Write-Host ""
    Write-Host "Options:" -ForegroundColor Yellow
    Write-Host "  -NoCache            (Image-Build only) Build Docker image without cache" -ForegroundColor White
    Write-Host "  -ForceUpdate        (Dev-Rebuild only) Pass Maven -U to re-check remote repos for" -ForegroundColor White
    Write-Host "                      updated snapshots/releases, without wiping the whole .m2 cache" -ForegroundColor White
    Write-Host ""
    Write-Host "Examples:" -ForegroundColor Yellow
    Write-Host "  ./dev.ps1 Image-Build           # Build backend image (uses cache)" -ForegroundColor White
    Write-Host "  ./dev.ps1 Image-Build -NoCache  # Build backend image without cache (forces full rebuild)" -ForegroundColor White
    Write-Host "  ./dev.ps1 Dev-Rebuild -ForceUpdate  # Rebuild + force Maven to re-check for updated deps" -ForegroundColor White
    Write-Host "  ./dev.ps1 Container-Stop        # Free port 8080 without a full rebuild-on-resume" -ForegroundColor White
    Write-Host ""
    Write-Host "The -NoCache flag is recommended if you suspect Docker is not picking up code changes." -ForegroundColor Yellow
}
# RAP Backend Development Scripts
#
# Usage:
#   ./dev.ps1 Image-Build           # Build backend image (uses cache)
#   ./dev.ps1 Image-Build -NoCache  # Build backend image without cache (forces full rebuild)
#
# The -NoCache flag is recommended if you suspect Docker is not picking up code changes.
# PowerShell automation for local development

# Run backend locally with hot reload (Spring Boot DevTools)
function Run {
    Write-Host "ERROR: JAVA_HOME is required to run mvnw directly" -ForegroundColor Red
    Write-Host ""
    Write-Host "To run backend with hot reload in VS Code:" -ForegroundColor Cyan
    Write-Host "  1. Make sure supporting services are running: .\dev.ps1 Support-Start" -ForegroundColor White
    Write-Host "  2. Press Ctrl+Shift+B (Run Build Task)" -ForegroundColor White
    Write-Host "  3. Select 'Run Spring Boot Dev Server'" -ForegroundColor White
    Write-Host ""
    Write-Host "This will run the backend with hot reload in a dedicated VS Code terminal." -ForegroundColor Yellow
    Write-Host "Changes to Java files will be automatically reloaded." -ForegroundColor Yellow
}

# Start supporting services only (database, keycloak, processes)
function Support-Start {
    Write-Host "Starting supporting services (database, keycloak, process)..." -ForegroundColor Cyan
    docker-compose up -d database keycloak process
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Supporting services started" -ForegroundColor Green
        Write-Host "" 
        Write-Host "Services running:" -ForegroundColor Cyan
        Write-Host "  - Database:  localhost:1433" -ForegroundColor White
        Write-Host "  - Keycloak:  localhost:9090" -ForegroundColor White
        Write-Host "  - Process:   localhost:8090" -ForegroundColor White
        Write-Host "" 
        Write-Host "Now run: .\dev.ps1 Run" -ForegroundColor Yellow
    } else {
        Write-Host "[ERROR] Failed to start supporting services" -ForegroundColor Red
    }
}

# Helper function to load environment variables from .env file
function Load-EnvFile {
    if (Test-Path .env) {
        Get-Content .env | ForEach-Object {
            if ($_ -match '^([^#][^=]+)=(.+)$') {
                $name = $matches[1].Trim()
                $value = $matches[2].Trim()
                [Environment]::SetEnvironmentVariable($name, $value, "Process")
                # Also set in current scope for immediate use
                Set-Variable -Name $name -Value $value -Scope Global
            }
        }
    }
}

# Setup script - Initialize environment
function Setup {
    Write-Host "Setting up development environment..." -ForegroundColor Cyan
    
    if (!(Test-Path .env)) {
        Copy-Item .env.example .env
        Write-Host "[OK] .env created from .env.example" -ForegroundColor Green
        Write-Host "  Please edit .env with your ACR credentials" -ForegroundColor Yellow
    } else {
        Write-Host "[OK] .env file already exists" -ForegroundColor Green
    }
}

# Login to Azure Container Registry
function ACR-Login {
    if (!(Test-Path .env)) {
        Write-Host "[ERROR] .env file not found. Run './dev.ps1 Setup' first" -ForegroundColor Red
        return
    }
    
    # Load environment variables
    Get-Content .env | ForEach-Object {
        if ($_ -match '^([^#][^=]+)=(.+)$') {
            [Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim(), "Process")
        }
    }
    
    $acrServer = $env:ACR_LOGIN_SERVER
    $acrUser = $env:ACR_USERNAME
    $acrPass = $env:ACR_PASSWORD
    
    if (!$acrServer -or !$acrUser -or !$acrPass) {
        Write-Host "[ERROR] ACR credentials not found in .env" -ForegroundColor Red
        return
    }
    
    Write-Host "Logging in to Azure Container Registry: $acrServer..." -ForegroundColor Cyan
    echo $acrPass | docker login $acrServer -u $acrUser --password-stdin
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Successfully logged in to ACR" -ForegroundColor Green
    } else {
        Write-Host "[ERROR] Failed to login to ACR" -ForegroundColor Red
    }
}

# Start backend + database only
function Dev-Start {
    Load-EnvFile
    Write-Host "Starting backend and database services..." -ForegroundColor Cyan
    Write-Host "Building backend image locally..." -ForegroundColor Yellow
    docker-compose up -d --build backend database
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "[OK] Services started!" -ForegroundColor Green
        Write-Host "  Backend:  http://localhost:8080" -ForegroundColor White
        Write-Host "  Database: localhost:1433" -ForegroundColor White
        Write-Host ""
        Write-Host "Use './dev.ps1 Logs' to view logs" -ForegroundColor Yellow
    } else {
        Write-Host "[ERROR] Failed to start services" -ForegroundColor Red
    }
}

# Start all services (full stack)
function Dev-Full {
    Load-EnvFile
    Write-Host "Starting all services (full stack)..." -ForegroundColor Cyan
    Write-Host "Building frontend and backend images locally..." -ForegroundColor Yellow
    docker-compose --profile full-stack up -d --build
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "[OK] All services started!" -ForegroundColor Green
        Write-Host "  Frontend:        http://localhost:4200" -ForegroundColor White
        Write-Host "  Backend:         http://localhost:8080" -ForegroundColor White
        Write-Host "  Process:         http://localhost:8090" -ForegroundColor White
        Write-Host "  Keycloak:        http://localhost:9090" -ForegroundColor White
        Write-Host "  Keycloak Admin:  http://localhost:9090/admin (admin/admin)" -ForegroundColor White
        Write-Host "  Database:        localhost:1433" -ForegroundColor White
        Write-Host ""
        Write-Host "Note: Keycloak takes ~60s to start. Check status with './dev.ps1 Logs keycloak'" -ForegroundColor Yellow
        Write-Host "Use './dev.ps1 Logs' to view logs" -ForegroundColor Yellow
    } else {
        Write-Host "[ERROR] Failed to start services" -ForegroundColor Red
    }
}

# Stop all services
function Dev-Stop {
    Write-Host "Stopping all services..." -ForegroundColor Cyan
    docker-compose down
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Services stopped" -ForegroundColor Green
    }
}

# Restart services
function Dev-Restart {
    Write-Host "Restarting services..." -ForegroundColor Cyan
    docker-compose restart
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Services restarted" -ForegroundColor Green
    }
}

# Rebuild backend and restart
function Dev-Rebuild {
    param([switch]$ForceUpdate)

    Load-EnvFile
    if ($ForceUpdate) {
        Write-Host "Rebuilding backend service (forcing Maven -U update check)..." -ForegroundColor Cyan
        docker-compose build --build-arg MAVEN_UPDATE_FLAG=-U backend
        docker-compose up -d backend
    } else {
        Write-Host "Rebuilding backend service..." -ForegroundColor Cyan
        docker-compose up -d --build backend
    }

    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Backend rebuilt and restarted" -ForegroundColor Green
    } else {
        Write-Host "[ERROR] Failed to rebuild backend" -ForegroundColor Red
    }
}

# Pause just the backend container (no teardown) — backend/docker-compose.yml
# bundles frontend + backend + keycloak + keycloak-db + database + process
# together, so Dev-Stop/Clean/Dev-Restart affect all of them. Use this to
# free port 8080 without touching anything else, or to stop just the backend
# for a quick pause/resume.
function Container-Stop {
    Write-Host "Stopping 'rap-backend' container (container + network are kept, not rebuilt on resume)..." -ForegroundColor Cyan
    docker stop rap-backend

    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Container stopped - resume with: .\dev.ps1 Container-Start" -ForegroundColor Green
    } else {
        Write-Host "[ERROR] Failed to stop container (is it running? try .\dev.ps1 Dev-Start)" -ForegroundColor Red
    }
}

# Resume a container previously paused with Container-Stop
function Container-Start {
    Write-Host "Resuming 'rap-backend' container..." -ForegroundColor Cyan
    docker start rap-backend

    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Container resumed" -ForegroundColor Green
    } else {
        Write-Host "[ERROR] Failed to resume container - it may not exist yet, try .\dev.ps1 Dev-Start" -ForegroundColor Red
    }
}

# Wipe the BuildKit Maven cache mount (use if the .m2 cache itself is
# suspected corrupt/stale — not needed just to pick up newer snapshot
# versions, which -ForceUpdate handles without losing the whole cache)
function Clean-Maven-Cache {
    Write-Host "Pruning BuildKit Maven (.m2) cache mounts..." -ForegroundColor Cyan
    docker builder prune --filter type=exec.cachemount -f

    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Maven cache mounts cleared" -ForegroundColor Green
    } else {
        Write-Host "[ERROR] Failed to clear Maven cache mounts" -ForegroundColor Red
    }
}

# View logs
function Dev-Logs {
    param([string]$Service = "")
    
    if ($Service) {
        Write-Host "Viewing logs for $Service..." -ForegroundColor Cyan
        docker-compose logs -f $Service
    } else {
        Write-Host "Viewing logs for all services..." -ForegroundColor Cyan
        docker-compose logs -f
    }
}

# Show container status
function Dev-Status {
    Write-Host ""
    Write-Host "=== Container Status ===" -ForegroundColor Cyan
    docker-compose ps
    Write-Host ""
    Write-Host "=== Service Health ===" -ForegroundColor Cyan
    docker ps --filter "name=rap-" --format "table {{.Names}}`t{{.Status}}"
}

# Initialize database
function DB-Init {
    Write-Host "Initializing database..." -ForegroundColor Cyan
    
    # Load DB password from .env
    Get-Content .env | ForEach-Object {
        if ($_ -match '^DB_PASSWORD=(.+)$') {
            $dbPassword = $matches[1]
        }
    }
    
    if (Test-Path init-scripts\01-create-database.sql) {
        docker exec -i rap-database /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P $dbPassword -C -i /docker-entrypoint-initdb.d/01-create-database.sql
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "[OK] Database initialized successfully" -ForegroundColor Green
        } else {
            Write-Host "[ERROR] Failed to initialize database" -ForegroundColor Red
        }
    } else {
        Write-Host "[ERROR] No init scripts found in init-scripts/" -ForegroundColor Red
    }
}

# Connect to database
function DB-Connect {
    # Load DB password from .env
    Get-Content .env | ForEach-Object {
        if ($_ -match '^DB_PASSWORD=(.+)$') {
            $dbPassword = $matches[1]
        }
    }
    
    Write-Host "Connecting to SQL Server..." -ForegroundColor Cyan
    docker exec -it rap-database /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P $dbPassword -C
}

# Reset database (DANGEROUS)
function DB-Reset {
    Write-Host "WARNING: This will delete all data in the database!" -ForegroundColor Red
    $confirmation = Read-Host "Type 'yes' to continue"
    
    if ($confirmation -eq 'yes') {
        Write-Host "Resetting database..." -ForegroundColor Cyan
        docker-compose down database
        docker volume rm backend_database-data -ErrorAction SilentlyContinue
        docker-compose up -d database
        Write-Host "[OK] Database reset complete. Run './dev.ps1 DB-Init' to initialize" -ForegroundColor Green
    } else {
        Write-Host "[ERROR] Database reset cancelled" -ForegroundColor Yellow
    }
}

# Repair Flyway checksum mismatches in the local database.
# Use this after editing an existing migration file (which you should avoid —
# prefer creating a new V<n+1> file instead). This syncs the stored checksums
# in flyway_schema_history to match the files currently on disk.
function Flyway-Repair {
    Write-Host "Running Flyway repair on local database..." -ForegroundColor Cyan
    Write-Host "This fixes 'Migration checksum mismatch' errors caused by editing applied migration files." -ForegroundColor Yellow

    # Build temporary container that runs Flyway repair against the local DB
    $flywayCmd = @(
        "run", "--rm",
        "--network", "backend_app-network",
        "-v", "${PWD}/src/main/resources/db/migration:/flyway/sql",
        "flyway/flyway:latest",
        "-url=jdbc:sqlserver://database:1433;databaseName=raptordb;encrypt=true;trustServerCertificate=true",
        "-user=sa",
        "-password=YourStrong@Passw0rd",
        "-schemas=RAP",
        "-defaultSchema=RAP",
        "-baselineOnMigrate=true",
        "repair"
    )

    docker @flywayCmd
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Flyway repair complete. You can now restart the backend." -ForegroundColor Green
        Write-Host "  Restart with: .\dev.ps1 Dev-Restart" -ForegroundColor White
    } else {
        Write-Host "[ERROR] Flyway repair failed. Check that the database container is running." -ForegroundColor Red
    }
}

# Clean up containers and networks
function Clean {
    Write-Host "Cleaning up containers and networks..." -ForegroundColor Cyan
    docker-compose down
    Write-Host "[OK] Cleanup complete" -ForegroundColor Green
}

# Clean everything including volumes
function Clean-All {
    Write-Host "WARNING: This will delete all containers, networks, AND VOLUMES (all data)!" -ForegroundColor Red
    $confirmation = Read-Host "Type 'yes' to continue"
    
    if ($confirmation -eq 'yes') {
        docker-compose down -v
        Write-Host "[OK] All containers, networks, and volumes removed" -ForegroundColor Green
    } else {
        Write-Host "[ERROR] Clean-all cancelled" -ForegroundColor Yellow
    }
}

# Build JAR locally
function Build {
    Write-Host "Building application..." -ForegroundColor Cyan
    
    if (Test-Path mvnw.cmd) {
        .\mvnw.cmd clean package -DskipTests
    } else {
        mvn clean package -DskipTests
    }
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Build complete! JAR is in target/" -ForegroundColor Green
    } else {
        Write-Host "[ERROR] Build failed" -ForegroundColor Red
    }
}

# Run tests
function Test {
    Write-Host "Running tests..." -ForegroundColor Cyan
    
    if (Test-Path mvnw.cmd) {
        .\mvnw.cmd test
    } else {
        mvn test
    }
}

# Build Docker image
function Image-Build {
    param(
        [switch]$NoCache
    )
    # Load ACR server from .env
    Get-Content .env | ForEach-Object {
        if ($_ -match '^ACR_LOGIN_SERVER=(.+)$') {
            $acrServer = $matches[1]
        }
    }
    Write-Host "Building Docker image..." -ForegroundColor Cyan
    $args = @()
    if ($NoCache.IsPresent) {
        $args += '--no-cache'
    }
    $args += '-t'
    $args += "${acrServer}/rap-backend:dev"
    $args += '.'
    docker build @args
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Image built: ${acrServer}/rap-backend:dev" -ForegroundColor Green
    } else {
        Write-Host "[ERROR] Image build failed" -ForegroundColor Red
    }
}

# Push image to ACR
function Image-Push {
    # Load environment variables
    Get-Content .env | ForEach-Object {
        if ($_ -match '^ACR_LOGIN_SERVER=(.+)$') {
            $acrServer = $matches[1]
        }
        if ($_ -match '^BACKEND_VERSION=(.+)$') {
            $version = $matches[1]
        }
    }
    
    Write-Host "Pushing image to ACR..." -ForegroundColor Cyan
    docker push ${acrServer}/rap-backend:${version}
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Image pushed successfully" -ForegroundColor Green
    } else {
        Write-Host "[ERROR] Image push failed" -ForegroundColor Red
    }
}

# Pull images from ACR
function Image-Pull {
    # Load environment variables
    Get-Content .env | ForEach-Object {
        if ($_ -match '^ACR_LOGIN_SERVER=(.+)$') {
            $acrServer = $matches[1]
        }
        if ($_ -match '^FRONTEND_VERSION=(.+)$') {
            $frontendVer = $matches[1]
        }
        if ($_ -match '^PROCESS_SERVICE_VERSION=(.+)$') {
            $processVer = $matches[1]
        }
    }
    
    Write-Host "Pulling images from ACR..." -ForegroundColor Cyan
    docker pull ${acrServer}/rap-frontend:${frontendVer}
    docker pull ${acrServer}/rap-process-service:${processVer}
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Images pulled successfully" -ForegroundColor Green
    } else {
        Write-Host "[ERROR] Failed to pull images" -ForegroundColor Red
    }
}

# Show help
function Show-Help {
    Write-Host ""
    Write-Host "RAP Backend Service - Development Commands" -ForegroundColor Cyan
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "USAGE: .\dev.ps1 <Command>" -ForegroundColor White
    Write-Host ""
    Write-Host "Setup Commands:" -ForegroundColor Yellow
    Write-Host "  Setup              - Initial setup (copy .env.example to .env)"
    Write-Host "  ACR-Login          - Login to Azure Container Registry"
    Write-Host ""
    Write-Host "Development Commands:" -ForegroundColor Yellow
    Write-Host "  Dev-Start          - Start backend + database only"
    Write-Host "  Dev-Full           - Start all services (frontend, backend, process, db)"
    Write-Host "  Dev-Stop           - Stop all services (note: this compose file bundles frontend/backend/keycloak/db/process together)"
    Write-Host "  Dev-Restart        - Restart all services"
    Write-Host "  Container-Stop     - Pause just the backend container (no rebuild on resume, doesn't touch other services)"
    Write-Host "  Container-Start    - Resume a container paused with Container-Stop"
    Write-Host "  Dev-Rebuild [-ForceUpdate] - Rebuild and restart backend"
    Write-Host "  Dev-Logs [service] - View logs (optionally for specific service)"
    Write-Host "  Dev-Status         - Show status of all containers"
    Write-Host "  Clean-Maven-Cache  - Wipe the BuildKit Maven (.m2) cache mount (corrupt/stale cache only)"
    Write-Host ""
    Write-Host "Database Commands:" -ForegroundColor Yellow
    Write-Host "  DB-Init            - Initialize database with init scripts"
    Write-Host "  DB-Connect         - Connect to SQL Server with sqlcmd"
    Write-Host "  DB-Reset           - Reset database (DANGER: deletes all data)"
    Write-Host ""
    Write-Host "Cleanup Commands:" -ForegroundColor Yellow
    Write-Host "  Clean              - Stop and remove all containers, networks"
    Write-Host "  Clean-All          - Clean + remove volumes (DANGER: deletes data)"
    Write-Host ""
    Write-Host "Build Commands:" -ForegroundColor Yellow
    Write-Host "  Build              - Build backend JAR locally (no Docker)"
    Write-Host "  Test               - Run tests"
    Write-Host "  Image-Build        - Build Docker image"
    Write-Host "  Image-Push         - Push image to ACR"
    Write-Host "  Image-Pull         - Pull latest images from ACR"
    Write-Host ""
    Write-Host "Examples:" -ForegroundColor Yellow
    Write-Host "  .\dev.ps1 Setup"
    Write-Host "  .\dev.ps1 Dev-Start"
    Write-Host "  .\dev.ps1 Dev-Logs backend"
    Write-Host "  .\dev.ps1 DB-Init"
    Write-Host "  .\dev.ps1 Dev-Rebuild -ForceUpdate"
    Write-Host "  .\dev.ps1 Container-Stop"
    Write-Host ""
}

# Main execution
if ($args.Count -eq 0) {
    Show-Help
} elseif ($args[0] -eq 'Help') {
    Help
} else {
    $command = $args[0]
    $additionalArgs = $args[1..($args.Length-1)]
    
    switch ($command) {
        "Run" { Run }
        "Support-Start" { Support-Start }
        "Setup" { Setup }
        "ACR-Login" { ACR-Login }
        "Dev-Start" { Dev-Start }
        "Dev-Full" { Dev-Full }
        "Dev-Stop" { Dev-Stop }
        "Dev-Restart" { Dev-Restart }
        "Container-Stop" { Container-Stop }
        "Container-Start" { Container-Start }
        "Dev-Rebuild" { Dev-Rebuild @additionalArgs }
        "Dev-Logs" { Dev-Logs @additionalArgs }
        "Dev-Status" { Dev-Status }
        "Clean-Maven-Cache" { Clean-Maven-Cache }
        "DB-Init" { DB-Init }
        "DB-Connect" { DB-Connect }
        "DB-Reset" { DB-Reset }
        "Clean" { Clean }
        "Clean-All" { Clean-All }
        "Build" { Build }
        "Test" { Test }
    "Image-Build" { Image-Build @additionalArgs }
        "Image-Push" { Image-Push }
        "Image-Pull" { Image-Pull }
        "Help" { Show-Help }
        default { 
            Write-Host "Unknown command: $command" -ForegroundColor Red
            Write-Host "Run '.\dev.ps1 Help' to see available commands" -ForegroundColor Yellow
        }
    }
}

