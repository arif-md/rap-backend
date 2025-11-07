-- ===================================================================
-- Flyway Migration: Authentication and Authorization Tables
-- ===================================================================
-- Description: Creates tables for OIDC authentication, user management,
--              role-based authorization, JWT refresh tokens, and token revocation
-- Version: V5
-- Author: System
-- Date: 2025-11-06
-- ===================================================================

-- ===================================================================
-- 1. USERS TABLE
-- ===================================================================
-- Stores authenticated users from OIDC provider
CREATE TABLE users (
    id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    
    -- OIDC Claims
    oidc_subject NVARCHAR(255) NOT NULL UNIQUE,  -- 'sub' claim from OIDC provider (permanent ID)
    email NVARCHAR(255) NOT NULL UNIQUE,          -- Email from OIDC
    full_name NVARCHAR(255),                      -- Display name from OIDC
    
    -- User Status
    is_active BIT NOT NULL DEFAULT 1,             -- Can be disabled by admin
    
    -- Audit Timestamps
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    last_login_at DATETIME2,                      -- Updated on each successful login
    
    -- Indexes for performance
    INDEX IX_users_email (email),
    INDEX IX_users_oidc_subject (oidc_subject),
    INDEX IX_users_is_active (is_active)
);

-- ===================================================================
-- 2. ROLES TABLE
-- ===================================================================
-- Application roles for authorization
CREATE TABLE roles (
    id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    role_name NVARCHAR(50) NOT NULL UNIQUE,       -- e.g., 'USER', 'ADMIN', 'MANAGER'
    description NVARCHAR(255),
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    
    INDEX IX_roles_role_name (role_name)
);

-- ===================================================================
-- 3. USER_ROLES TABLE (Many-to-Many)
-- ===================================================================
-- Maps users to their assigned roles
CREATE TABLE user_roles (
    id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    user_id UNIQUEIDENTIFIER NOT NULL,
    role_id UNIQUEIDENTIFIER NOT NULL,
    granted_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    granted_by NVARCHAR(255),                     -- Admin who granted the role (for audit)
    
    CONSTRAINT FK_user_roles_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT FK_user_roles_role FOREIGN KEY (role_id) 
        REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT UQ_user_role UNIQUE (user_id, role_id),
    
    INDEX IX_user_roles_user_id (user_id),
    INDEX IX_user_roles_role_id (role_id)
);

-- ===================================================================
-- 4. REFRESH_TOKENS TABLE
-- ===================================================================
-- Stores refresh tokens for session extension without re-authentication
CREATE TABLE refresh_tokens (
    id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    token_hash NVARCHAR(255) NOT NULL UNIQUE,     -- SHA-256 hash of refresh token (never store plain token)
    user_id UNIQUEIDENTIFIER NOT NULL,
    
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
        REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX IX_refresh_tokens_token_hash (token_hash),
    INDEX IX_refresh_tokens_user_id (user_id),
    INDEX IX_refresh_tokens_expires_at (expires_at)
);

-- ===================================================================
-- 5. REVOKED_TOKENS TABLE (Optional - Advanced)
-- ===================================================================
-- Tracks revoked JWT tokens for immediate invalidation
-- Note: Only needed if you want instant JWT revocation before natural expiration
CREATE TABLE revoked_tokens (
    id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    jti NVARCHAR(255) NOT NULL UNIQUE,            -- JWT ID from token claim
    user_id UNIQUEIDENTIFIER NOT NULL,
    
    -- Token Metadata
    revoked_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    expires_at DATETIME2 NOT NULL,                -- When JWT would have expired naturally
    reason NVARCHAR(255),                         -- 'LOGOUT', 'SECURITY_BREACH', 'ADMIN_REVOKE'
    revoked_by NVARCHAR(255),                     -- Who/what revoked it
    
    CONSTRAINT FK_revoked_tokens_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX IX_revoked_tokens_jti (jti),
    INDEX IX_revoked_tokens_user_id (user_id),
    INDEX IX_revoked_tokens_expires_at (expires_at)
);

-- ===================================================================
-- 6. SEED DATA - Default Roles
-- ===================================================================
-- Insert standard application roles
INSERT INTO roles (id, role_name, description) VALUES
    (NEWID(), 'USER', 'Basic authenticated user with read access'),
    (NEWID(), 'MANAGER', 'Manager with read/write access to managed entities'),
    (NEWID(), 'ADMIN', 'System administrator with full access');

-- ===================================================================
-- 7. SEED DATA - Test User (for local development only)
-- ===================================================================
-- Create a test user for local development
-- In production, users are created automatically on first OIDC login
DECLARE @testUserId UNIQUEIDENTIFIER = NEWID();
DECLARE @userRoleId UNIQUEIDENTIFIER;

-- Insert test user
INSERT INTO users (id, oidc_subject, email, full_name, is_active)
VALUES (
    @testUserId,
    'test|local-dev-user',                        -- Fake OIDC subject for local testing
    'test@example.com',
    'Test User',
    1
);

-- Assign USER role to test user
SELECT @userRoleId = id FROM roles WHERE role_name = 'USER';
INSERT INTO user_roles (id, user_id, role_id, granted_by)
VALUES (NEWID(), @testUserId, @userRoleId, 'SYSTEM_SEED');

-- ===================================================================
-- 8. CLEANUP JOBS (Comments - for future scheduled tasks)
-- ===================================================================
-- These should be run periodically to clean up expired tokens

-- Clean up expired refresh tokens (older than 30 days)
-- DELETE FROM refresh_tokens WHERE expires_at < DATEADD(day, -30, GETUTCDATE());

-- Clean up expired revoked JWT tokens (no longer needed after natural expiration)
-- DELETE FROM revoked_tokens WHERE expires_at < GETUTCDATE();

-- ===================================================================
-- Migration Complete
-- ===================================================================
