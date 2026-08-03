-- =============================================================================
-- Flyway Migration V10: Create ATTACHMENTS Table
-- =============================================================================
-- Maps a generated/uploaded file (e.g. a PDF produced by processes' PDFWorkItemHandler)
-- to its owning application. storage_location is an opaque identifier resolved by
-- x.y.z.backend.storage.AttachmentStorageService (a relative path locally, a blob name
-- in Azure) - never returned to API clients directly, only the download endpoint uses it.
-- =============================================================================
IF NOT EXISTS (SELECT 1 FROM sys.tables t JOIN sys.schemas s ON t.schema_id = s.schema_id WHERE s.name = 'RAP' AND t.name = 'ATTACHMENTS')
BEGIN
    CREATE TABLE RAP.ATTACHMENTS (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        application_id BIGINT NOT NULL,
        file_name NVARCHAR(255) NOT NULL,
        content_type NVARCHAR(100) NOT NULL DEFAULT 'application/pdf',
        storage_location NVARCHAR(1000) NOT NULL,
        file_size_bytes BIGINT NULL,
        created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
        created_by BIGINT NULL,
        CONSTRAINT FK_attachments_application FOREIGN KEY (application_id)
            REFERENCES RAP.application(id),
        CONSTRAINT FK_attachments_created_by FOREIGN KEY (created_by)
            REFERENCES RAP.USER_INFO(id),

        INDEX idx_attachments_application_id (application_id)
    );
    PRINT 'Created table: RAP.ATTACHMENTS';
END
ELSE
    PRINT 'Table RAP.ATTACHMENTS already exists';
GO

IF EXISTS (
    SELECT 1 FROM fn_listextendedproperty(
        N'MS_Description',
        N'SCHEMA', N'RAP',
        N'TABLE', N'ATTACHMENTS',
        NULL, NULL)
)
    EXEC sp_updateextendedproperty
        @name = N'MS_Description',
        @value = N'Maps a stored file (e.g. a generated PDF) to its owning application',
        @level0type = N'SCHEMA', @level0name = 'RAP',
        @level1type = N'TABLE', @level1name = 'ATTACHMENTS';
ELSE
    EXEC sp_addextendedproperty
        @name = N'MS_Description',
        @value = N'Maps a stored file (e.g. a generated PDF) to its owning application',
        @level0type = N'SCHEMA', @level0name = 'RAP',
        @level1type = N'TABLE', @level1name = 'ATTACHMENTS';
