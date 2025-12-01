-- =============================================================================
-- Flyway Migration V13: Create RAP Schema and Configure Default Schema
-- =============================================================================
-- This migration creates the RAP schema to isolate backend tables from
-- jBPM tables (which use the JBPM schema). This allows both services to
-- share the same SQL Server database without table name conflicts.
--
-- Schema isolation strategy:
-- - RAP schema: Backend application tables (users, roles, applications, etc.)
-- - JBPM schema: Process engine tables (managed by jBPM service)
-- =============================================================================

-- Step 1: Create RAP schema if it doesn't exist
IF NOT EXISTS (SELECT * FROM sys.schemas WHERE name = 'RAP')
BEGIN
    EXEC('CREATE SCHEMA RAP');
    PRINT 'Created RAP schema';
END
ELSE
BEGIN
    PRINT 'RAP schema already exists';
END
GO

-- Step 2: Set RAP as default schema for 'sa' user (local development)
-- This allows MyBatis queries without explicit schema prefixes to work correctly
-- Note: In Azure, the managed identity user's default schema will be set via Azure configuration
IF EXISTS (SELECT * FROM sys.database_principals WHERE name = 'sa')
BEGIN
    ALTER USER sa WITH DEFAULT_SCHEMA = RAP;
    PRINT 'Set RAP as default schema for sa user';
END
GO

-- Step 3: For Azure managed identity (if exists)
-- The managed identity principal name varies, so we check for common patterns
DECLARE @principalName NVARCHAR(128);
DECLARE @sql NVARCHAR(MAX);

-- Check if there's a user that looks like a managed identity (contains app name or GUID pattern)
SELECT TOP 1 @principalName = name 
FROM sys.database_principals 
WHERE type IN ('E', 'X') -- External user or External group
  AND is_fixed_role = 0
  AND name NOT IN ('guest', 'dbo', 'INFORMATION_SCHEMA', 'sys', 'sa');

IF @principalName IS NOT NULL
BEGIN
    SET @sql = 'ALTER USER [' + @principalName + '] WITH DEFAULT_SCHEMA = RAP';
    EXEC sp_executesql @sql;
    PRINT 'Set RAP as default schema for managed identity: ' + @principalName;
END
GO

-- Note: After this migration, all tables created by Flyway will be in the RAP schema
-- (due to spring.flyway.default-schema=RAP configuration)
-- All MyBatis queries will default to the RAP schema (due to user's default schema being RAP)

PRINT 'RAP schema configuration completed successfully';
PRINT 'Backend tables will be isolated in RAP schema, separate from JBPM schema';
GO
