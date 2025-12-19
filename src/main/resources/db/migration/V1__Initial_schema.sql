-- =============================================================================
-- Flyway Migration V1: Initial Schema
-- =============================================================================
-- This migration creates the RAP schema and initial database schema.
-- It runs automatically on first startup (both Azure and local Docker).
--
-- Schema isolation:
-- - RAP schema: Backend application tables (USER_INFO, applications, tasks, etc.)
-- - JBPM schema: Process engine tables (managed by processes service)
--
-- Flyway tracks which migrations have run in the flyway_schema_history table.
-- =============================================================================

-- Create RAP schema if it doesn't exist
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

-- Set RAP as default schema for sa user (local development)
-- This allows MyBatis queries without explicit schema prefixes to work correctly
IF EXISTS (SELECT * FROM sys.database_principals WHERE name = 'sa')
BEGIN
    ALTER USER sa WITH DEFAULT_SCHEMA = RAP;
    PRINT 'Set RAP as default schema for sa user';
END
GO

-- Set RAP as default schema for Azure managed identity (if exists)
-- The managed identity principal name varies, so we check for common patterns
DECLARE @principalName NVARCHAR(128);
DECLARE @sql NVARCHAR(MAX);

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

-- From this point, all tables will be created in RAP schema
-- Note: We explicitly qualify with RAP. to ensure correct schema regardless of connection defaults

-- Actual application tables are created in subsequent migrations:
-- - V2: Application table
-- - V5: Authentication tables (USER_INFO, roles, tokens)
-- - V6: Task table
-- - V7: Permit table

-- Success message
PRINT 'RAP schema created successfully!';
