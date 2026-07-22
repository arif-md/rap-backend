# Database Schema Isolation Strategy

## Overview

The RAP application uses **schema-based isolation** to separate backend application tables from jBPM process engine tables within a shared SQL Server database. This approach provides logical separation while allowing both services to access the same database instance.

## Schema Architecture

```
SQL Server Database: rapdb (localhost:1433)
├── RAP Schema
│   ├── USER_INFO
│   ├── roles
│   ├── user_roles
│   ├── applications
│   ├── tasks
│   ├── permits
│   └── flyway_schema_history (Flyway migrations tracking)
│
└── JBPM Schema
    ├── processinstanceinfo
    ├── task
    ├── variableinstanceinfo
    └── ... (other jBPM tables)
```

## Configuration Details

### Backend Service (MyBatis)

**File:** `backend/src/main/resources/application.properties`

```properties
# Flyway creates all tables in RAP schema
spring.flyway.schemas=RAP
spring.flyway.default-schema=RAP

# MyBatis configuration (uses RAP schema via user's default schema)
mybatis.mapper-locations=classpath:mapper/**/*.xml
mybatis.type-aliases-package=x.y.z.backend.domain.model
```

**Migration:** `V13__Create_RAP_schema.sql`
- Creates RAP schema if not exists
- Sets RAP as default schema for database users (sa for local, managed identity for Azure)
- Ensures all subsequent Flyway migrations create tables in RAP schema

**How it works:**
1. Flyway's `default-schema=RAP` ensures all migrations run in RAP schema
2. SQL migration sets user's default schema to RAP
3. MyBatis queries don't need explicit schema prefixes (defaults to RAP)

### Processes Service (jBPM + Hibernate)

**File:** `processes/src/main/resources/application.properties`

```properties
# JPA/Hibernate default schema configuration
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServer2012Dialect
spring.jpa.properties.hibernate.default_schema=JBPM
```

**How it works:**
- Hibernate's `default_schema=JBPM` property automatically prefixes all table names
- jBPM creates all process engine tables in JBPM schema
- No SQL migrations needed (jBPM manages its own schema via Hibernate DDL)

## Local Development Setup

### Initial Database Setup

1. **Start SQL Server container** (via backend or processes dev scripts):
   ```powershell
   cd backend
   .\dev.ps1 Dev-Start
   ```

2. **Verify schemas created**:
   ```sql
   -- Connect to localhost:1433, database: rapdb
   SELECT name FROM sys.schemas WHERE name IN ('RAP', 'JBPM');
   ```

3. **Check backend tables**:
   ```sql
   SELECT TABLE_SCHEMA, TABLE_NAME 
   FROM INFORMATION_SCHEMA.TABLES 
   WHERE TABLE_SCHEMA = 'RAP'
   ORDER BY TABLE_NAME;
   ```

4. **Check jBPM tables**:
   ```sql
   SELECT TABLE_SCHEMA, TABLE_NAME 
   FROM INFORMATION_SCHEMA.TABLES 
   WHERE TABLE_SCHEMA = 'JBPM'
   ORDER BY TABLE_NAME;
   ```

### Default Schema Verification

Check which schema is default for your user:

```sql
-- Check default schema for current user
SELECT name, default_schema_name 
FROM sys.database_principals 
WHERE name = USER_NAME();

-- Verify sa user's default schema
SELECT name, default_schema_name 
FROM sys.database_principals 
WHERE name = 'sa';
```

Expected result: `default_schema_name = 'RAP'` for backend connections

## Azure Deployment

### Azure SQL Configuration

**Backend connection string** (with Managed Identity):
```
jdbc:sqlserver://<server>.database.windows.net:1433;databaseName=rapdb;authentication=ActiveDirectoryMSI;
```

**Processes connection string** (with Managed Identity):
```
jdbc:sqlserver://<server>.database.windows.net:1433;databaseName=rapdb;authentication=ActiveDirectoryMSI;
```

### Managed Identity Schema Setup

The `V13__Create_RAP_schema.sql` migration automatically detects and configures the managed identity user's default schema:

```sql
-- Migration automatically finds managed identity principal
-- and sets default schema to RAP
ALTER USER [<managed-identity-name>] WITH DEFAULT_SCHEMA = RAP;
```

## Benefits of Schema Isolation

1. **Logical Separation**: Clear ownership boundaries between services
2. **No Table Name Conflicts**: Each service can use natural table names
3. **Independent Permissions**: Can grant schema-level permissions separately
4. **Easier Backup/Restore**: Can backup/restore schemas independently
5. **Clear Audit Trail**: Schema-qualified queries show which service accessed data
6. **Simplified Troubleshooting**: Easy to identify which service owns which tables

## Migration from Single Schema

If you have existing tables in the `dbo` schema that need to be moved to `RAP`:

```sql
-- Move existing table to RAP schema
ALTER SCHEMA RAP TRANSFER dbo.users;
ALTER SCHEMA RAP TRANSFER dbo.roles;
ALTER SCHEMA RAP TRANSFER dbo.user_roles;
-- ... repeat for all tables
```

**Note:** This is only needed if tables already exist in `dbo` schema. For new deployments, Flyway creates all tables in RAP schema automatically.

## Troubleshooting

### Backend queries failing with "Invalid object name"

**Symptom:** MyBatis queries fail with errors like `Invalid object name 'users'`

**Cause:** User's default schema not set to RAP

**Solution:** Run V13 migration or manually set:
```sql
ALTER USER sa WITH DEFAULT_SCHEMA = RAP;
```

### Tables created in wrong schema

**Symptom:** New tables appear in `dbo` instead of `RAP`

**Cause:** Flyway default-schema not configured

**Solution:** Verify `application.properties`:
```properties
spring.flyway.default-schema=RAP
```

### jBPM tables in wrong schema

**Symptom:** jBPM tables appear in `dbo` instead of `JBPM`

**Cause:** Hibernate default_schema not configured

**Solution:** Verify `processes/application.properties`:
```properties
spring.jpa.properties.hibernate.default_schema=JBPM
```

### Cross-schema queries

If you need to query across schemas (e.g., backend querying process data):

```xml
<!-- In MyBatis mapper XML -->
<select id="findProcessByUser" resultType="ProcessInfo">
    SELECT p.id, p.name
    FROM JBPM.processinstanceinfo p
    WHERE p.initiator = #{userId}
</select>
```

**Best Practice:** Use schema-qualified names (`JBPM.tablename`) for cross-schema queries.

## Testing Schema Isolation

### Verify Backend Uses RAP Schema

```powershell
# Start backend service
cd backend
.\dev.ps1 Dev-Start

# Check logs for table creation
.\dev.ps1 Dev-Logs backend

# Verify via SQL
SELECT * FROM RAP.USER_INFO;  # Should work
SELECT * FROM USER_INFO;       # Should work (default schema is RAP)
SELECT * FROM dbo.USER_INFO;   # Should fail (table doesn't exist in dbo)
```

### Verify Processes Uses JBPM Schema

```powershell
# Start processes service
cd processes
.\dev.ps1 Dev-Start

# Check logs for jBPM initialization
.\dev.ps1 Dev-Logs processes

# Verify via SQL
SELECT * FROM JBPM.processinstanceinfo;  # Should work (after first process deployment)
SELECT * FROM processinstanceinfo;        # May fail (default schema not JBPM)
```

## References

- [SQL Server Schemas Documentation](https://docs.microsoft.com/sql/relational-databases/security/authentication-access/schemas)
- [Flyway Schema Configuration](https://flywaydb.org/documentation/configuration/parameters/defaultSchema)
- [Hibernate default_schema Property](https://docs.jboss.org/hibernate/orm/5.6/userguide/html_single/Hibernate_User_Guide.html#configurations-mapping)
- [MyBatis Configuration](https://mybatis.org/mybatis-3/configuration.html)

## Summary

| Service | ORM | Schema | Configuration Method |
|---------|-----|--------|---------------------|
| Backend | MyBatis | RAP | Flyway `default-schema` + User default schema |
| Processes | Hibernate/JPA | JBPM | Hibernate `default_schema` property |

---

## Flyway Migration Rules

### The Golden Rule: Never Edit an Applied Migration

Once a migration file (e.g. `V2__Create_auth_tables.sql`) has been applied to **any** environment — including your local Docker database — the file is **frozen**. Editing it changes its checksum, causing this error on the next startup:

```
Validate failed: Migrations have failed validation
Migration checksum mismatch for migration version 4
-> Applied to database : 1566870783
-> Resolved locally    : 1943800922
```

**Instead, always create a new migration:**

```
src/main/resources/db/migration/
  V2__Create_auth_tables.sql       ← FROZEN. Never touch.
  V5__Create_task_table.sql        ← FROZEN.
  V10__Add_missing_column.sql      ← New migration for your schema change.
```

### Local Docker: Validation is Relaxed by Design

`application-docker.properties` sets:
```properties
spring.flyway.validate-on-migrate=false
```

This prevents local container crashes during active development when a migration file may be edited during iteration. **This only applies to the `docker` profile** — all Azure profiles (`dev`, `test`, `prod`) inherit `validate-on-migrate=true` from `application.properties` and will catch mismatches before deployment.

### Fixing a Checksum Mismatch on Your Local DB

If you have already edited an applied migration and the backend container won't start, you have two options:

**Option 1 — Repair (preserves existing data):**
```powershell
cd backend
.\dev.ps1 Flyway-Repair
.\dev.ps1 Dev-Restart
```
This syncs the stored checksums in `flyway_schema_history` to match the files currently on disk. Use this when the schema change is backward-compatible and data should be kept.

**Option 2 — Reset (destroys all local data):**
```powershell
cd backend
.\dev.ps1 DB-Reset
```
Use this when you want a clean slate. All data is lost.

**Option 3 — Manual SQL (targeted fix):**
```powershell
# Find the new checksum from the Flyway error message, then:
docker exec rap-database /opt/mssql-tools18/bin/sqlcmd `
  -S localhost -U sa -P "YourStrong@Passw0rd" -C `
  -Q "USE raptordb; UPDATE RAP.flyway_schema_history SET checksum = <new_checksum> WHERE version = '<version>';"
docker restart rap-backend
```

### Profile Isolation: Local vs Azure

The codebase uses Spring profiles to ensure Azure-specific services are never required locally:

| Setting | `application.properties` (base) | `application-docker.properties` | `application-dev.properties` |
|---------|----------------------------------|----------------------------------|-------------------------------|
| App Config | `enabled=false` | (inherits false) | `enabled=true` |
| Key Vault | disabled | (inherits disabled) | enabled via bootstrap |
| Flyway validation | `validate-on-migrate=true` | `validate-on-migrate=false` | (inherits true) |
| SQL auth | SQL Server container | SQL Server container | Managed Identity |

**Rule:** When adding a new Azure service integration, always:
1. Default it to `false`/disabled in `application.properties`
2. Enable it only in the Azure profile properties (`application-dev.properties`, `bootstrap-dev.properties`)
3. Provide local defaults for any required env vars in `docker-compose.yml` using `${VAR:-default}`

Both services share the same database (`rapdb`) but maintain isolation through schema separation. This provides the benefits of a shared database (connection pooling, transactions) with the logical separation of independent databases.
