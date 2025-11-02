-- =============================================================================
-- Flyway Migration V2: Create APPLICATION Table
-- =============================================================================
-- This migration creates the APPLICATION table for managing application records.
-- =============================================================================

CREATE TABLE application (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    application_name NVARCHAR(255) NOT NULL,
    application_code NVARCHAR(100) NOT NULL UNIQUE,
    description NVARCHAR(MAX),
    status NVARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    owner_name NVARCHAR(255),
    owner_email NVARCHAR(255),
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    created_by NVARCHAR(100),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_by NVARCHAR(100),
    
    -- Indexes for common query patterns
    INDEX idx_application_code (application_code),
    INDEX idx_application_name (application_name),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);

-- Add check constraint for status values
ALTER TABLE application 
ADD CONSTRAINT chk_application_status 
CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING', 'ARCHIVED'));

-- Comments for documentation
EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Application registry table storing metadata about applications in the system', 
    @level0type = N'SCHEMA', @level0name = 'dbo',
    @level1type = N'TABLE', @level1name = 'application';
