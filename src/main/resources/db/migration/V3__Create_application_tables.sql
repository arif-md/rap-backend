-- =============================================================================
-- Flyway Migration V3: Create APPLICATION Table
-- =============================================================================
-- This migration creates the APPLICATION table for managing application records.
-- =============================================================================
IF NOT EXISTS (SELECT 1 FROM sys.tables t JOIN sys.schemas s ON t.schema_id = s.schema_id WHERE s.name = 'RAP' AND t.name = 'application')
BEGIN
    CREATE TABLE RAP.application (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        application_name NVARCHAR(255) NOT NULL,
        application_code NVARCHAR(100) NOT NULL UNIQUE,
        description NVARCHAR(MAX),
        status NVARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
        owner_name NVARCHAR(255),
        owner_email NVARCHAR(255),
        university_id BIGINT NULL,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        created_by NVARCHAR(100),
        updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_by NVARCHAR(100),
        CONSTRAINT FK_application_university_id FOREIGN KEY (university_id) 
            REFERENCES RAP.university(id),
        CONSTRAINT chk_application_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING', 'ARCHIVED')),

        -- Indexes for common query patterns
        INDEX idx_application_code (application_code),
        INDEX idx_application_name (application_name),
        INDEX idx_status (status),
        INDEX idx_created_at (created_at),
        INDEX idx_application_university_id (university_id)
    );
    PRINT 'Created table: RAP.application';
END
ELSE
  PRINT 'Table RAP.application already exists';
GO

-- Comments for documentation
IF EXISTS (
    SELECT 1 FROM fn_listextendedproperty(
        N'MS_Description',
        N'SCHEMA', N'RAP',
        N'TABLE', N'application',
        NULL, NULL)
)
    EXEC sp_updateextendedproperty 
        @name = N'MS_Description', 
        @value = N'Application registry table storing metadata about applications in the system', 
        @level0type = N'SCHEMA', @level0name = 'RAP',
        @level1type = N'TABLE', @level1name = 'application';
ELSE
    EXEC sp_addextendedproperty 
        @name = N'MS_Description', 
        @value = N'Application registry table storing metadata about applications in the system', 
        @level0type = N'SCHEMA', @level0name = 'RAP',
        @level1type = N'TABLE', @level1name = 'application';