-- Create permit table
IF NOT EXISTS (SELECT 1 FROM sys.tables t JOIN sys.schemas s ON t.schema_id = s.schema_id WHERE s.name = 'RAP' AND t.name = 'permit')
BEGIN
    CREATE TABLE RAP.permit (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        permit_number NVARCHAR(50) NOT NULL UNIQUE,
        permit_type NVARCHAR(100) NOT NULL,
        status NVARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
        issue_date DATE NOT NULL,
        expiry_date DATE,
        holder_id BIGINT NOT NULL,
        description NVARCHAR(1000),
        university_id BIGINT NULL,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        created_by NVARCHAR(255) NOT NULL,
        updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_by NVARCHAR(255) NOT NULL,
        CONSTRAINT FK_permit_university_id FOREIGN KEY (university_id) 
            REFERENCES RAP.university(id),
        INDEX idx_permit_university_id (university_id),
        INDEX IDX_permit_holder_id (holder_id),
        INDEX IDX_permit_number (permit_number),
        INDEX IDX_permit_status (status),
        INDEX IDX_permit_expiry_date (expiry_date)
    );
    PRINT 'Created table: RAP.permit';
END
ELSE
  PRINT 'Table RAP.permit already exists';
GO
