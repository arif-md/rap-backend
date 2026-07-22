-- ===================================================================
-- Flyway Migration: Authentication and Authorization Tables
-- ===================================================================
-- Description: Creates tables for OIDC authentication, user management,
--              role-based authorization, JWT refresh tokens, and token revocation
-- Version: V4
-- Author: System
-- Date: 2025-12-18
-- ===================================================================
-- NOTE: In Azure deployments, the bootstrap-schemas.sql deployment script
-- creates these tables BEFORE containers start. The IF NOT EXISTS guards
-- make this migration a safe no-op when tables already exist.
-- In local Docker dev, this migration is the primary source of table creation.
-- ===================================================================

-- ============================================================================
-- 2. Base Tables - RAP Schema
--    (definitions match infra bootstrap-schemas.sql exactly)
-- ============================================================================

-- 2.1 University reference table
IF NOT EXISTS (SELECT 1 FROM sys.tables t JOIN sys.schemas s ON t.schema_id = s.schema_id WHERE s.name = 'RAP' AND t.name = 'university')
BEGIN
    CREATE TABLE RAP.university (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        university_name NVARCHAR(255) NOT NULL,
        university_code NVARCHAR(50) NOT NULL UNIQUE,
        status NVARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),

        INDEX idx_university_name (university_name),
        INDEX idx_university_code (university_code),
        INDEX idx_university_status (status)
    );
    PRINT 'Created table: RAP.university';
END
ELSE
    PRINT 'Table RAP.university already exists';
GO

-- ===================================================================
-- 2.2 USER_INFO TABLE
-- ===================================================================
-- Stores authenticated users from OIDC provider
IF NOT EXISTS (SELECT 1 FROM sys.tables t JOIN sys.schemas s ON t.schema_id = s.schema_id WHERE s.name = 'RAP' AND t.name = 'USER_INFO')
BEGIN
    CREATE TABLE RAP.USER_INFO (
        id BIGINT IDENTITY PRIMARY KEY,    
        -- OIDC Claims
        oidc_subject NVARCHAR(255) NOT NULL UNIQUE,  -- 'sub' claim from OIDC provider (permanent ID)
        email NVARCHAR(255) NOT NULL UNIQUE,          -- Email from OIDC
        first_name NVARCHAR(255) null,
        last_name NVARCHAR(255) null,
        full_name NVARCHAR(255),                      -- Display name from OIDC
        lang NVARCHAR(255) null,
        pwd NVARCHAR(255) null, 
        -- User Status
        is_active BIT NOT NULL DEFAULT 1,             -- Can be disabled by admin 
        -- Audit Timestamps
        created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
        updated_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
        last_login_at DATETIME2,                      -- Updated on each successful login
        -- Indexes for performance
        INDEX IX_user_info_email (email),
        INDEX IX_user_info_oidc_subject (oidc_subject),
        INDEX IX_user_info_is_active (is_active)
    );
    PRINT 'Created table: RAP.USER_INFO';
END
ELSE
    PRINT 'Table RAP.USER_INFO already exists (created by bootstrap)';
GO

-- ===================================================================
-- 2.3. ROLES TABLE
-- ===================================================================
-- Application roles for authorization
IF NOT EXISTS (SELECT 1 FROM sys.tables t JOIN sys.schemas s ON t.schema_id = s.schema_id WHERE s.name = 'RAP' AND t.name = 'ROLE_REF')
BEGIN
    CREATE TABLE RAP.ROLE_REF (
        id BIGINT IDENTITY PRIMARY KEY,
        role_name NVARCHAR(50) NOT NULL UNIQUE,       -- e.g., 'USER', 'ADMIN', 'MANAGER'
        description NVARCHAR(255),
        created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),    
        INDEX IX_roles_role_name (role_name)
    );
    PRINT 'Created table: RAP.ROLE_REF';
END
ELSE
    PRINT 'Table RAP.ROLE_REF already exists (created by bootstrap)';
GO

-- ===================================================================
-- 2.4. USER_ROLES TABLE (Many-to-Many)
-- ===================================================================
-- Maps users to their assigned roles
IF NOT EXISTS (SELECT 1 FROM sys.tables t JOIN sys.schemas s ON t.schema_id = s.schema_id WHERE s.name = 'RAP' AND t.name = 'USER_ROLE')
BEGIN
    CREATE TABLE RAP.USER_ROLE (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        role_id BIGINT NOT NULL,
        university_id BIGINT NULL, 
        granted_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
        granted_by NVARCHAR(255),                     -- Admin who granted the role (for audit)
        CONSTRAINT FK_user_roles_user FOREIGN KEY (user_id) 
            REFERENCES RAP.USER_INFO(id) ON DELETE CASCADE,
        CONSTRAINT FK_user_roles_role FOREIGN KEY (role_id) 
            REFERENCES RAP.ROLE_REF(id) ON DELETE CASCADE,
        CONSTRAINT FK_user_roles_university_id FOREIGN KEY (university_id) 
            REFERENCES RAP.university(id),
        CONSTRAINT UQ_user_role UNIQUE (user_id, role_id, university_id),
        
        INDEX IX_user_roles_user_id (user_id),
        INDEX IX_user_roles_role_id (role_id),
        INDEX IX_user_roles_university_id (university_id)
    );
    PRINT 'Created table: RAP.USER_ROLE';
END
ELSE
    PRINT 'Table RAP.USER_ROLE already exists (created by bootstrap)';
GO

-- ===================================================================
-- 2.5 REFRESH_TOKENS TABLE
-- ===================================================================
-- Stores refresh tokens for session extension without re-authentication
IF NOT EXISTS (SELECT 1 FROM sys.tables t JOIN sys.schemas s ON t.schema_id = s.schema_id WHERE s.name = 'RAP' AND t.name = 'refresh_tokens')
BEGIN
    CREATE TABLE RAP.refresh_tokens (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        token_hash NVARCHAR(255) NOT NULL UNIQUE,     -- SHA-256 hash of refresh token (never store plain token)
        user_id BIGINT NOT NULL,
        
        -- Token Lifecycle
        issued_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
        expires_at DATETIME2 NOT NULL,                -- Typically 7 days from issue
        last_used_at DATETIME2,                       -- Updated when used to refresh JWT
        
        -- Security Metadata
        ip_address NVARCHAR(45),                      -- IPv4 or IPv6
        user_agent NVARCHAR(500),                     -- Browser/client info
        
        -- Revocation
        is_revoked BIT NOT NULL DEFAULT 0,
        revoked_at DATETIME2,
        revoked_reason NVARCHAR(255),                 -- 'LOGOUT', 'SECURITY_BREACH', 'PASSWORD_CHANGE'
        
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
GO

-- ===================================================================
-- 2.6 REVOKED_TOKENS TABLE (Optional - Advanced)
-- ===================================================================
-- Tracks revoked JWT tokens for immediate invalidation
-- Note: Only needed if you want instant JWT revocation before natural expiration
IF NOT EXISTS (SELECT 1 FROM sys.tables t JOIN sys.schemas s ON t.schema_id = s.schema_id WHERE s.name = 'RAP' AND t.name = 'revoked_tokens')
BEGIN
    CREATE TABLE RAP.revoked_tokens (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        jti NVARCHAR(255) NOT NULL UNIQUE,            -- JWT ID from token claim
        user_id BIGINT NOT NULL,
        
        -- Token Metadata
        revoked_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
        expires_at DATETIME2 NOT NULL,                -- When JWT would have expired naturally
        reason NVARCHAR(255),                         -- 'LOGOUT', 'SECURITY_BREACH', 'ADMIN_REVOKE'
        revoked_by NVARCHAR(255),                     -- Who/what revoked it
        
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
GO

-- ===================================================================
-- 8. CLEANUP JOBS (Comments - for future scheduled tasks)
-- ===================================================================
-- These should be run periodically to clean up expired tokens

-- Clean up expired refresh tokens (older than 30 days)
-- DELETE FROM RAP.refresh_tokens WHERE expires_at < DATEADD(day, -30, GETUTCDATE());

-- Clean up expired revoked JWT tokens (no longer needed after natural expiration)
-- DELETE FROM RAP.revoked_tokens WHERE expires_at < GETUTCDATE();

-- ===================================================================
-- Migration Complete
-- ===================================================================
