-- ===================================================================
-- 7. SEED DATA - Test User (for local development only, idempotent)
-- ===================================================================
-- Create a test user for local development
-- In production, users are created automatically on first OIDC login
IF NOT EXISTS (SELECT 1 FROM RAP.USER_INFO WHERE oidc_subject = 'system|system-user')
BEGIN
    DECLARE @testUserId BIGINT;
    DECLARE @userRoleId BIGINT;

    -- Insert pre-defined users (IDENTITY will auto-generate ID)
    INSERT INTO RAP.USER_INFO (oidc_subject, email, full_name, is_active)
    VALUES (
        'system|system-user',                        -- Fake OIDC subject for local testing
        'system@nexgeninc.com',
        'System User',
        1
    );
    SET @testUserId = SCOPE_IDENTITY();

    -- Assign ADMIN role to test user
    SELECT @userRoleId = id FROM RAP.ROLE_REF WHERE role_name = 'ADMIN';
    INSERT INTO RAP.USER_ROLE (user_id, role_id, granted_by)
    VALUES (@testUserId, @userRoleId, 'SYSTEM_SEED');
    PRINT 'Seeded test user: system@nexgeninc.com';
END
ELSE
    PRINT 'Test user system@nexgeninc.com already exists (seeded by bootstrap)';
GO


-- =============================================================================
-- Flyway Migration V9: Seed initial internal (Azure AD SSO) user
-- =============================================================================
-- AzureAdOidcUserService requires internal users to exist in the database
-- before they can log in via Azure AD SSO. This script provisions the
-- initial administrator with the INTERNAL_USER role.
-- =============================================================================

-- 1. Insert user into USER_INFO (if not already present)
IF NOT EXISTS (SELECT 1 FROM RAP.USER_INFO WHERE email = 'arif.mohammed@nexgeninc.com')
BEGIN
    INSERT INTO RAP.USER_INFO (oidc_subject, email, full_name, is_active)
    VALUES (
        'aad|arif.mohammed@nexgeninc.com',   -- synthetic OIDC subject (updated on first real login)
        'arif.mohammed@nexgeninc.com',
        'Arif Mohammed',
        1
    );
    PRINT 'Inserted internal user arif.mohammed@nexgeninc.com';
END
ELSE
BEGIN
    PRINT 'Internal user arif.mohammed@nexgeninc.com already exists';
END

-- 2. Assign INTERNAL_USER role (if not already assigned)
IF NOT EXISTS (
    SELECT 1
    FROM RAP.USER_ROLE ur
    INNER JOIN RAP.USER_INFO u  ON ur.user_id = u.id
    INNER JOIN RAP.ROLE_REF r   ON ur.role_id = r.id
    WHERE u.email = 'arif.mohammed@nexgeninc.com'
      AND r.role_name = 'INTERNAL_USER'
)
BEGIN
    INSERT INTO RAP.USER_ROLE (user_id, role_id, granted_by)
    SELECT u.id, r.id, 'flyway-migration-V9'
    FROM RAP.USER_INFO u
    CROSS JOIN RAP.ROLE_REF r
    WHERE u.email = 'arif.mohammed@nexgeninc.com'
      AND r.role_name = 'INTERNAL_USER';
    PRINT 'Assigned INTERNAL_USER role to arif.mohammed@nexgeninc.com';
END
ELSE
BEGIN
    PRINT 'INTERNAL_USER role already assigned to arif.mohammed@nexgeninc.com';
END
