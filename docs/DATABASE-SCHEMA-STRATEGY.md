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

Both services share the same database (`rapdb`) but maintain isolation through schema separation. This provides the benefits of a shared database (connection pooling, transactions) with the logical separation of independent databases.
