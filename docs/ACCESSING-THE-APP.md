# Accessing the Application

## Available Endpoints

Once the application is running, you can access the following endpoints:

### Public Endpoints (No Authentication Required)

**Actuator Health Check:**
```
http://localhost:8080/actuator/health
```

### Protected Endpoints (Authentication Required)

All other endpoints require authentication with:
- **Username**: `admin` (from `.env` file)
- **Password**: `admin123` (from `.env` file)

**API Root:**
```
http://localhost:8080/api/
```
Returns welcome message with application info.

**API Health:**
```
http://localhost:8080/api/health
```
Returns service health status.

## How to Access

### Option 1: Web Browser

1. Open your browser and go to: `http://localhost:8080/api/`
2. You'll be prompted for credentials:
   - Username: `admin`
   - Password: `admin123`
3. After authentication, you'll see the API response

### Option 2: cURL (PowerShell)

**With authentication:**
```powershell
# Using basic auth
curl -u admin:admin123 http://localhost:8080/api/

# Or using headers
$headers = @{
    Authorization = "Basic " + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("admin:admin123"))
}
Invoke-WebRequest -Uri http://localhost:8080/api/ -Headers $headers
```

**Public health endpoint (no auth needed):**
```powershell
curl http://localhost:8080/actuator/health
```

### Option 3: Postman

1. Create a new GET request
2. URL: `http://localhost:8080/api/`
3. Go to "Authorization" tab
4. Select "Basic Auth"
5. Username: `admin`
6. Password: `admin123`
7. Send the request

### Option 4: Invoke-RestMethod (PowerShell)

```powershell
# Create credentials
$secpasswd = ConvertTo-SecureString "admin123" -AsPlainText -Force
$credential = New-Object System.Management.Automation.PSCredential ("admin", $secpasswd)

# Make request
Invoke-RestMethod -Uri http://localhost:8080/api/ -Credential $credential -Method Get
```

## Testing the Application

### Check if Container is Running

```powershell
docker-compose ps
```

Expected output:
```
NAME             IMAGE            STATUS         PORTS
spring-backend   backend:latest   Up (healthy)   0.0.0.0:8080->8080/tcp
sqlserver        mssql/...        Up (healthy)   0.0.0.0:1433->1433/tcp
```

### View Application Logs

```powershell
docker-compose logs -f backend
```

Look for:
```
Tomcat started on port 8080 (http) with context path '/'
Started BackendApplication in X.XXX seconds
```

### Test Endpoints

```powershell
# Test public actuator endpoint
Invoke-RestMethod http://localhost:8080/actuator/health

# Test protected API endpoint
$cred = @{
    Username = "admin"
    Password = "admin123"
}
Invoke-RestMethod http://localhost:8080/api/ -Authentication Basic -Credential (
    New-Object PSCredential($cred.Username, (ConvertTo-SecureString $cred.Password -AsPlainText -Force))
)
```

## Expected Responses

### /actuator/health
```json
{
  "status": "UP"
}
```

### /api/
```json
{
  "message": "Welcome to Spring Boot Backend API",
  "status": "UP",
  "timestamp": "2025-10-31T12:34:56.789",
  "version": "0.0.1-SNAPSHOT"
}
```

### /api/health
```json
{
  "status": "healthy",
  "service": "backend-api"
}
```

## Troubleshooting

### "Connection refused" or "Cannot connect"

1. Check if container is running:
   ```powershell
   docker ps
   ```

2. Check container logs:
   ```powershell
   docker-compose logs backend
   ```

3. Verify port is not blocked:
   ```powershell
   netstat -ano | findstr :8080
   ```

### "401 Unauthorized"

- Make sure you're using the correct credentials:
  - Username: `admin`
  - Password: `admin123`
- Check `.env` file for the actual values

### "404 Not Found"

- Ensure you're accessing the correct path:
  - `/api/` - for API root
  - `/api/health` - for health check
  - `/actuator/health` - for actuator health (no auth needed)
- Note the trailing slashes

### Application not starting

1. Check logs:
   ```powershell
   docker-compose logs backend
   ```

2. Check if SQL Server is healthy:
   ```powershell
   docker ps
   ```

3. Restart services:
   ```powershell
   docker-compose restart
   ```

## Security Notes

### Development vs Production

**Development (.env):**
```
SPRING_SECURITY_USER_NAME=admin
SPRING_SECURITY_USER_PASSWORD=admin123
```

**Production (Azure):**
- Use Azure Key Vault for passwords
- Use strong, randomly generated passwords
- Consider OAuth2/JWT for API authentication
- Enable HTTPS/TLS

### Changing Credentials

1. Edit `.env` file:
   ```
   SPRING_SECURITY_USER_NAME=myuser
   SPRING_SECURITY_USER_PASSWORD=MySecure@Pass123
   ```

2. Restart containers:
   ```powershell
   docker-compose restart
   ```

## Next Steps

1. ✅ Application is running
2. ✅ Basic endpoints available
3. 📝 Add your business logic controllers
4. 📝 Create database entities and repositories
5. 📝 Add more API endpoints
6. 📝 Configure database migrations
7. 📝 Set up integration tests

For more information, see:
- `README-DOCKER.md` - Docker setup guide
- `ENVIRONMENT-VARIABLES.md` - Environment configuration
