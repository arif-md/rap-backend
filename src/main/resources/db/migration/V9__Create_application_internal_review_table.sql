-- =============================================================================
-- Flyway Migration V9: Create APPLICATION_INTERNAL_REVIEW Table
-- =============================================================================
-- Stores the internal reviewer's signature + review date captured when an internal
-- user completes a "My Tasks" review task from the internal application review page.
-- signature_image is stored as raw PNG bytes (decoded from the canvas data URL) rather
-- than base64 text, so it can be embedded directly (e.g. via iText's Image.getInstance)
-- when this data is later rendered onto a generated PDF.
-- =============================================================================
IF NOT EXISTS (SELECT 1 FROM sys.tables t JOIN sys.schemas s ON t.schema_id = s.schema_id WHERE s.name = 'RAP' AND t.name = 'application_internal_review')
BEGIN
    CREATE TABLE RAP.application_internal_review (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        application_id BIGINT NOT NULL,
        task_id BIGINT NOT NULL,
        reviewer_user_id BIGINT NOT NULL,
        review_date DATE NOT NULL,
        signature_image VARBINARY(MAX) NOT NULL,
        created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
        CONSTRAINT FK_app_internal_review_application FOREIGN KEY (application_id)
            REFERENCES RAP.application(id),
        CONSTRAINT FK_app_internal_review_user FOREIGN KEY (reviewer_user_id)
            REFERENCES RAP.USER_INFO(id),

        INDEX idx_app_internal_review_application_id (application_id),
        INDEX idx_app_internal_review_task_id (task_id)
    );
    PRINT 'Created table: RAP.application_internal_review';
END
ELSE
    PRINT 'Table RAP.application_internal_review already exists';
GO

IF EXISTS (
    SELECT 1 FROM fn_listextendedproperty(
        N'MS_Description',
        N'SCHEMA', N'RAP',
        N'TABLE', N'application_internal_review',
        NULL, NULL)
)
    EXEC sp_updateextendedproperty
        @name = N'MS_Description',
        @value = N'Internal reviewer signature + review date captured when completing an internal review task',
        @level0type = N'SCHEMA', @level0name = 'RAP',
        @level1type = N'TABLE', @level1name = 'application_internal_review';
ELSE
    EXEC sp_addextendedproperty
        @name = N'MS_Description',
        @value = N'Internal reviewer signature + review date captured when completing an internal review task',
        @level0type = N'SCHEMA', @level0name = 'RAP',
        @level1type = N'TABLE', @level1name = 'application_internal_review';
