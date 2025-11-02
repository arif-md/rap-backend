# Quick Start - Application CRUD API

## Start Backend (Local)

```powershell
# 1. Start database
docker-compose up -d sqlserver

# 2. Run backend
cd backend
./mvnw spring-boot:run

# 3. Wait for: "Started BackendApplication"
```

## Test Endpoints

### Create Application
```powershell
curl -X POST http://localhost:8080/api/applications `
  -H "Content-Type: application/json" `
  -d '{"applicationName":"Test App","applicationCode":"TST-001","status":"ACTIVE","createdBy":"admin","updatedBy":"admin"}'
```

### Get All Applications
```powershell
curl http://localhost:8080/api/applications
```

### Get by ID
```powershell
curl http://localhost:8080/api/applications/1
```

### Update Application
```powershell
curl -X PUT http://localhost:8080/api/applications/1 `
  -H "Content-Type: application/json" `
  -d '{"applicationName":"Updated App","applicationCode":"TST-001","status":"ACTIVE","updatedBy":"admin"}'
```

### Delete Application
```powershell
curl -X DELETE http://localhost:8080/api/applications/1
```

### Search by Name
```powershell
curl "http://localhost:8080/api/applications/search?name=Test"
```

### Filter by Status
```powershell
curl http://localhost:8080/api/applications/status/ACTIVE
```

## Architecture

```
ApplicationController     → ApplicationService      → ApplicationHandler        → ApplicationMapper
(@RestController)           (@Service @Transactional)  (@Component)                (@Repository)
                                                                                   ↓
                                                                           MyBatis SQL Queries
                                                                                   ↓
                                                                           Azure SQL Database
```

## Status Values
- `ACTIVE` - Active application
- `INACTIVE` - Disabled application
- `PENDING` - Awaiting approval
- `ARCHIVED` - Historical record

## Files Modified
- ✅ `pom.xml` - Dependencies (MyBatis, Azure SQL, Security)
- ✅ `BackendApplication.java` - Added @MapperScan
- ✅ `application.properties` - Database + MyBatis config
- ✅ `SecurityConfig.java` - Multiple filter chains (disabled)

## Files Created
- ✅ `V2__Create_application_table.sql` - Database schema
- ✅ `Application.java` - POJO domain model
- ✅ `ApplicationMapper.java` + `.xml` - MyBatis data access
- ✅ `ApplicationHandler.java` - Business logic
- ✅ `ApplicationService.java` - Transaction boundary
- ✅ `ApplicationController.java` - REST endpoints
- ✅ `TESTING-GUIDE.md` - Full testing guide
- ✅ `IMPLEMENTATION-SUMMARY.md` - This summary

## Troubleshooting

**Port 8080 in use?**
```powershell
netstat -ano | findstr :8080
taskkill /PID <pid> /F
```

**Database connection failed?**
```powershell
docker ps
docker logs rap-prototype-sqlserver-1
docker-compose restart sqlserver
```

**Reset database?**
```powershell
docker-compose down -v
docker-compose up -d sqlserver
```

## Next: Azure Deployment

```bash
# Grant DB access to Managed Identity
CREATE USER [dev-rap-be] FROM EXTERNAL PROVIDER;
ALTER ROLE db_datareader ADD MEMBER [dev-rap-be];
ALTER ROLE db_datawriter ADD MEMBER [dev-rap-be];
ALTER ROLE db_ddladmin ADD MEMBER [dev-rap-be];

# Deploy
azd deploy backend
```

See **TESTING-GUIDE.md** for complete instructions!
