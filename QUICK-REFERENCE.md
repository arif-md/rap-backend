# RAP Backend - Quick Command Reference

## For Windows Users (PowerShell - No Extra Tools Needed!)

```powershell
# First Time Setup
.\dev.ps1 Setup          # Create .env file
notepad .env             # Add your ACR credentials
.\dev.ps1 ACR-Login      # Login to registry

# Daily Development
.\dev.ps1 Dev-Start      # Start backend + database
.\dev.ps1 Dev-Rebuild    # Rebuild after code changes
.\dev.ps1 Dev-Logs       # View logs
.\dev.ps1 Dev-Stop       # Stop everything

# Database
.\dev.ps1 DB-Init        # Initialize database
.\dev.ps1 DB-Connect     # Connect to SQL Server

# Help
.\dev.ps1 Help           # Show all commands
```

## For Mac/Linux Users (Make)

```bash
# First Time Setup
make setup               # Create .env file
nano .env                # Add your ACR credentials
make acr-login           # Login to registry

# Daily Development
make dev-start           # Start backend + database
make dev-rebuild         # Rebuild after code changes
make dev-logs            # View logs
make dev-stop            # Stop everything

# Database
make db-init             # Initialize database
make db-connect          # Connect to SQL Server

# Help
make help                # Show all commands
```

## URLs

- Backend: http://localhost:8080
- Health: http://localhost:8080/actuator/health
- API: http://localhost:8080/api/
- Database: localhost:1433

## Default Credentials

- Spring Security: admin / admin123
- SQL Server: sa / YourStrong@Passw0rd (from .env)

## Documentation

- [README.md](README.md) - Quick start and overview
- [LOCAL-DEVELOPMENT.md](LOCAL-DEVELOPMENT.md) - Complete development guide
- [DOCKER-INTERNALS.md](DOCKER-INTERNALS.md) - Docker deep dive
