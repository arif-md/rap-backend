IF NOT EXISTS (SELECT 1 FROM sys.tables t JOIN sys.schemas s ON t.schema_id = s.schema_id WHERE s.name = 'RAP' AND t.name = 'refresh_tokens')
BEGIN
    CREATE TABLE RAP.refresh_tokens (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        token_hash NVARCHAR(255) NOT NULL UNIQUE,
        user_id BIGINT NOT NULL,
        issued_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
        expires_at DATETIME2 NOT NULL,
        last_used_at DATETIME2,
        ip_address NVARCHAR(45),
        user_agent NVARCHAR(500),
        is_revoked BIT NOT NULL DEFAULT 0,
        revoked_at DATETIME2,
        revoked_reason NVARCHAR(255),
        CONSTRAINT FK_refresh_tokens_user FOREIGN KEY (user_id)
            REFERENCES RAP.USER_INFO(id) ON DELETE CASCADE,
        INDEX IX_refresh_tokens_token_hash (token_hash),
        INDEX IX_refresh_tokens_user_id (user_id),
        INDEX IX_refresh_tokens_expires_at (expires_at)
    );
    PRINT 'Created table: RAP.refresh_tokens';
END
ELSE
    PRINT 'Table RAP.refresh_tokens already exists (created by bootstrap)';

-- ===================================================================
-- 5. REVOKED_TOKENS TABLE
-- ===================================================================
IF NOT EXISTS (SELECT 1 FROM sys.tables t JOIN sys.schemas s ON t.schema_id = s.schema_id WHERE s.name = 'RAP' AND t.name = 'revoked_tokens')
BEGIN
    CREATE TABLE RAP.revoked_tokens (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        jti NVARCHAR(255) NOT NULL UNIQUE,
        user_id BIGINT NOT NULL,
        revoked_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
        expires_at DATETIME2 NOT NULL,
        reason NVARCHAR(255),
        revoked_by NVARCHAR(255),
        CONSTRAINT FK_revoked_tokens_user FOREIGN KEY (user_id)
            REFERENCES RAP.USER_INFO(id) ON DELETE CASCADE,
        INDEX IX_revoked_tokens_jti (jti),
        INDEX IX_revoked_tokens_user_id (user_id),
        INDEX IX_revoked_tokens_expires_at (expires_at)
    );
    PRINT 'Created table: RAP.revoked_tokens';
END
ELSE
    PRINT 'Table RAP.revoked_tokens already exists (created by bootstrap)';

-- ===================================================================
-- Migration Complete
-- ===================================================================
