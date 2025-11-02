# =============================================================================
# RAP Backend Service - Development Makefile
# =============================================================================
# Convenient commands for local development with Docker Compose
# Works with PowerShell on Windows

.PHONY: help setup dev-start dev-stop dev-restart dev-rebuild dev-logs dev-status clean acr-login

# Default target
.DEFAULT_GOAL := help

# Load environment variables from .env file
ifneq (,$(wildcard .env))
    include .env
    export
endif

# -----------------------------------------------------------------------------
# Help
# -----------------------------------------------------------------------------
help: ## Show this help message
	@echo "RAP Backend Service - Development Commands"
	@echo "==========================================="
	@echo ""
	@echo "Setup Commands:"
	@echo "  make setup          - Initial setup (copy .env.example to .env)"
	@echo "  make acr-login      - Login to Azure Container Registry"
	@echo ""
	@echo "Development Commands:"
	@echo "  make dev-start      - Start backend + database only"
	@echo "  make dev-full       - Start all services (frontend, backend, process, db)"
	@echo "  make dev-stop       - Stop all services"
	@echo "  make dev-restart    - Restart all services"
	@echo "  make dev-rebuild    - Rebuild and restart backend"
	@echo "  make dev-logs       - View logs from all services"
	@echo "  make dev-status     - Show status of all containers"
	@echo ""
	@echo "Database Commands:"
	@echo "  make db-init        - Initialize database with init scripts"
	@echo "  make db-connect     - Connect to SQL Server with sqlcmd"
	@echo "  make db-reset       - Reset database (DANGER: deletes all data)"
	@echo ""
	@echo "Cleanup Commands:"
	@echo "  make clean          - Stop and remove all containers, networks"
	@echo "  make clean-all      - Clean + remove volumes (DANGER: deletes data)"
	@echo ""
	@echo "Build Commands:"
	@echo "  make build          - Build backend JAR locally (no Docker)"
	@echo "  make test           - Run tests"
	@echo ""

# -----------------------------------------------------------------------------
# Setup
# -----------------------------------------------------------------------------
setup: ## Initial setup - create .env from .env.example
	@if not exist .env ( \
		echo Creating .env file from .env.example... && \
		copy .env.example .env && \
		echo .env created! Please edit it with your ACR credentials. \
	) else ( \
		echo .env file already exists. \
	)

acr-login: ## Login to Azure Container Registry
	@echo Logging in to Azure Container Registry...
	@echo $(ACR_PASSWORD) | docker login $(ACR_LOGIN_SERVER) -u $(ACR_USERNAME) --password-stdin
	@echo Successfully logged in to $(ACR_LOGIN_SERVER)

# -----------------------------------------------------------------------------
# Development - Backend + Database Only
# -----------------------------------------------------------------------------
dev-start: ## Start backend and database only
	@echo Starting backend and database services...
	docker-compose up -d backend database
	@echo ""
	@echo Services started!
	@echo Backend:  http://localhost:8080
	@echo Database: localhost:1433
	@echo ""
	@echo Use 'make dev-logs' to view logs

dev-full: ## Start all services (frontend, backend, process-service, database)
	@echo Starting all services...
	docker-compose --profile full-stack up -d
	@echo ""
	@echo All services started!
	@echo Frontend:        http://localhost:4200
	@echo Backend:         http://localhost:8080
	@echo Process Service: http://localhost:8090
	@echo Database:        localhost:1433
	@echo ""
	@echo Use 'make dev-logs' to view logs

dev-stop: ## Stop all services
	@echo Stopping all services...
	docker-compose down
	@echo Services stopped.

dev-restart: ## Restart all services
	@echo Restarting services...
	docker-compose restart
	@echo Services restarted.

dev-rebuild: ## Rebuild backend image and restart
	@echo Rebuilding backend service...
	docker-compose up -d --build backend
	@echo Backend rebuilt and restarted.

dev-logs: ## View logs from all running services
	docker-compose logs -f

dev-logs-backend: ## View backend logs only
	docker-compose logs -f backend

dev-logs-db: ## View database logs only
	docker-compose logs -f database

dev-status: ## Show status of all containers
	@echo ""
	@echo "=== Container Status ==="
	@docker-compose ps
	@echo ""
	@echo "=== Service Health ==="
	@docker ps --filter "name=rap-" --format "table {{.Names}}\t{{.Status}}"

# -----------------------------------------------------------------------------
# Database Management
# -----------------------------------------------------------------------------
db-init: ## Initialize database with SQL scripts
	@echo Initializing database...
	@if exist init-scripts\01-create-database.sql ( \
		docker exec -i rap-database /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$(DB_PASSWORD)" -C -i /docker-entrypoint-initdb.d/01-create-database.sql && \
		echo Database initialized successfully. \
	) else ( \
		echo No init scripts found in init-scripts/ \
	)

db-connect: ## Connect to SQL Server with sqlcmd
	@echo Connecting to SQL Server...
	docker exec -it rap-database /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$(DB_PASSWORD)" -C

db-reset: ## Reset database (DANGER: deletes all data)
	@echo WARNING: This will delete all data in the database!
	@echo Press Ctrl+C to cancel, or wait 5 seconds to continue...
	@timeout /t 5 /nobreak > nul
	docker-compose down database
	docker volume rm backend_database-data
	docker-compose up -d database
	@echo Database reset complete. Use 'make db-init' to initialize.

# -----------------------------------------------------------------------------
# Cleanup
# -----------------------------------------------------------------------------
clean: ## Stop and remove containers, networks
	@echo Cleaning up containers and networks...
	docker-compose down
	@echo Cleanup complete.

clean-all: ## Remove everything including volumes (DANGER: deletes data)
	@echo WARNING: This will delete all containers, networks, AND VOLUMES (all data)!
	@echo Press Ctrl+C to cancel, or wait 5 seconds to continue...
	@timeout /t 5 /nobreak > nul
	docker-compose down -v
	@echo All containers, networks, and volumes removed.

# -----------------------------------------------------------------------------
# Build & Test (without Docker)
# -----------------------------------------------------------------------------
build: ## Build the application JAR locally
	@echo Building application...
	@if exist mvnw.cmd ( \
		.\mvnw.cmd clean package -DskipTests \
	) else ( \
		mvn clean package -DskipTests \
	)
	@echo Build complete! JAR is in target/

test: ## Run tests
	@echo Running tests...
	@if exist mvnw.cmd ( \
		.\mvnw.cmd test \
	) else ( \
		mvn test \
	)

# -----------------------------------------------------------------------------
# Docker Image Management
# -----------------------------------------------------------------------------
image-build: ## Build Docker image manually
	@echo Building Docker image...
	docker build -t $(ACR_LOGIN_SERVER)/rap-backend:dev .
	@echo Image built: $(ACR_LOGIN_SERVER)/rap-backend:dev

image-push: ## Push image to ACR
	@echo Pushing image to ACR...
	docker push $(ACR_LOGIN_SERVER)/rap-backend:$(BACKEND_VERSION)
	@echo Image pushed successfully.

image-pull: ## Pull latest images from ACR
	@echo Pulling images from ACR...
	docker pull $(ACR_LOGIN_SERVER)/rap-frontend:$(FRONTEND_VERSION)
	docker pull $(ACR_LOGIN_SERVER)/rap-process-service:$(PROCESS_SERVICE_VERSION)
	@echo Images pulled successfully.
