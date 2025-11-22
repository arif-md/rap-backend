# Show help for all commands
function Help {
    Write-Host "RAP Backend Development Scripts" -ForegroundColor Cyan
    Write-Host "" 
    Write-Host "Usage:" -ForegroundColor White
    Write-Host "  ./dev.ps1 <Command> [Options]" -ForegroundColor White
    Write-Host "" 
    Write-Host "Commands:" -ForegroundColor Yellow
    Write-Host "  Dev-Start           Start backend and database services" -ForegroundColor White
    Write-Host "  Dev-Full            Start all services (full stack)" -ForegroundColor White
    Write-Host "  Dev-Stop            Stop all services" -ForegroundColor White
    Write-Host "  Dev-Restart         Restart all services" -ForegroundColor White
    Write-Host "  Dev-Rebuild         Rebuild backend and restart" -ForegroundColor White
    Write-Host "  Dev-Logs [service]  View logs for a service or all" -ForegroundColor White
    Write-Host "  Image-Build [-NoCache]  Build backend Docker image (use -NoCache to force full rebuild)" -ForegroundColor White
    Write-Host "  Image-Push          Push backend image to ACR" -ForegroundColor White
    Write-Host "  DB-Init             Initialize database" -ForegroundColor White
    Write-Host "  DB-Connect          Connect to database" -ForegroundColor White
    Write-Host "  DB-Reset            Reset database (DANGEROUS)" -ForegroundColor White
    Write-Host "  Clean               Clean up containers and networks" -ForegroundColor White
    Write-Host "  Clean-All           Remove all containers, networks, and volumes" -ForegroundColor White
    Write-Host "" 
    Write-Host "Options:" -ForegroundColor Yellow
    Write-Host "  -NoCache            (Image-Build only) Build Docker image without cache" -ForegroundColor White
    Write-Host "" 
    Write-Host "Examples:" -ForegroundColor Yellow
    Write-Host "  ./dev.ps1 Image-Build           # Build backend image (uses cache)" -ForegroundColor White
    Write-Host "  ./dev.ps1 Image-Build -NoCache  # Build backend image without cache (forces full rebuild)" -ForegroundColor White
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
        Write-Host "✓ .env created from .env.example" -ForegroundColor Green
        Write-Host "  Please edit .env with your ACR credentials" -ForegroundColor Yellow
    } else {
        Write-Host "✓ .env file already exists" -ForegroundColor Green
    }
}

# Login to Azure Container Registry
function ACR-Login {
    if (!(Test-Path .env)) {
        Write-Host "✗ .env file not found. Run './dev.ps1 Setup' first" -ForegroundColor Red
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
        Write-Host "✗ ACR credentials not found in .env" -ForegroundColor Red
        return
    }
    
    Write-Host "Logging in to Azure Container Registry: $acrServer..." -ForegroundColor Cyan
    echo $acrPass | docker login $acrServer -u $acrUser --password-stdin
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Successfully logged in to ACR" -ForegroundColor Green
    } else {
        Write-Host "✗ Failed to login to ACR" -ForegroundColor Red
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
        Write-Host "✓ Services started!" -ForegroundColor Green
        Write-Host "  Backend:  http://localhost:8080" -ForegroundColor White
        Write-Host "  Database: localhost:1433" -ForegroundColor White
        Write-Host ""
        Write-Host "Use './dev.ps1 Logs' to view logs" -ForegroundColor Yellow
    } else {
        Write-Host "✗ Failed to start services" -ForegroundColor Red
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
        Write-Host "✓ All services started!" -ForegroundColor Green
        Write-Host "  Frontend:        http://localhost:4200" -ForegroundColor White
        Write-Host "  Backend:         http://localhost:8080" -ForegroundColor White
        Write-Host "  Keycloak:        http://localhost:9090" -ForegroundColor White
        Write-Host "  Keycloak Admin:  http://localhost:9090/admin (admin/admin)" -ForegroundColor White
        Write-Host "  Database:        localhost:1433" -ForegroundColor White
        Write-Host ""
        Write-Host "Note: Keycloak takes ~60s to start. Check status with './dev.ps1 Logs keycloak'" -ForegroundColor Yellow
        Write-Host "Note: Process Service (port 8090) is disabled - not available in ACR" -ForegroundColor Yellow
        Write-Host "Use './dev.ps1 Logs' to view logs" -ForegroundColor Yellow
    } else {
        Write-Host "✗ Failed to start services" -ForegroundColor Red
    }
}

# Stop all services
function Dev-Stop {
    Write-Host "Stopping all services..." -ForegroundColor Cyan
    docker-compose down
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Services stopped" -ForegroundColor Green
    }
}

# Restart services
function Dev-Restart {
    Write-Host "Restarting services..." -ForegroundColor Cyan
    docker-compose restart
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Services restarted" -ForegroundColor Green
    }
}

# Rebuild backend and restart
function Dev-Rebuild {
    Load-EnvFile
    Write-Host "Rebuilding backend service..." -ForegroundColor Cyan
    docker-compose up -d --build backend
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Backend rebuilt and restarted" -ForegroundColor Green
    } else {
        Write-Host "✗ Failed to rebuild backend" -ForegroundColor Red
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
            Write-Host "✓ Database initialized successfully" -ForegroundColor Green
        } else {
            Write-Host "✗ Failed to initialize database" -ForegroundColor Red
        }
    } else {
        Write-Host "✗ No init scripts found in init-scripts/" -ForegroundColor Red
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
        Write-Host "✓ Database reset complete. Run './dev.ps1 DB-Init' to initialize" -ForegroundColor Green
    } else {
        Write-Host "✗ Database reset cancelled" -ForegroundColor Yellow
    }
}

# Clean up containers and networks
function Clean {
    Write-Host "Cleaning up containers and networks..." -ForegroundColor Cyan
    docker-compose down
    Write-Host "✓ Cleanup complete" -ForegroundColor Green
}

# Clean everything including volumes
function Clean-All {
    Write-Host "WARNING: This will delete all containers, networks, AND VOLUMES (all data)!" -ForegroundColor Red
    $confirmation = Read-Host "Type 'yes' to continue"
    
    if ($confirmation -eq 'yes') {
        docker-compose down -v
        Write-Host "✓ All containers, networks, and volumes removed" -ForegroundColor Green
    } else {
        Write-Host "✗ Clean-all cancelled" -ForegroundColor Yellow
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
        Write-Host "✓ Build complete! JAR is in target/" -ForegroundColor Green
    } else {
        Write-Host "✗ Build failed" -ForegroundColor Red
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
        Write-Host "✓ Image built: ${acrServer}/rap-backend:dev" -ForegroundColor Green
    } else {
        Write-Host "✗ Image build failed" -ForegroundColor Red
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
        Write-Host "✓ Image pushed successfully" -ForegroundColor Green
    } else {
        Write-Host "✗ Image push failed" -ForegroundColor Red
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
        Write-Host "✓ Images pulled successfully" -ForegroundColor Green
    } else {
        Write-Host "✗ Failed to pull images" -ForegroundColor Red
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
    Write-Host "  Dev-Stop           - Stop all services"
    Write-Host "  Dev-Restart        - Restart all services"
    Write-Host "  Dev-Rebuild        - Rebuild and restart backend"
    Write-Host "  Dev-Logs [service] - View logs (optionally for specific service)"
    Write-Host "  Dev-Status         - Show status of all containers"
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
        "Setup" { Setup }
        "ACR-Login" { ACR-Login }
        "Dev-Start" { Dev-Start }
        "Dev-Full" { Dev-Full }
        "Dev-Stop" { Dev-Stop }
        "Dev-Restart" { Dev-Restart }
        "Dev-Rebuild" { Dev-Rebuild }
        "Dev-Logs" { Dev-Logs @additionalArgs }
        "Dev-Status" { Dev-Status }
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
