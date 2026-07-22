-- Seed universities
IF NOT EXISTS (SELECT 1 FROM RAP.university WHERE university_code = 'HARVARD')
    INSERT INTO RAP.university (university_name, university_code) VALUES ('Harvard University', 'HARVARD');
IF NOT EXISTS (SELECT 1 FROM RAP.university WHERE university_code = 'STANFORD')
    INSERT INTO RAP.university (university_name, university_code) VALUES ('Stanford University', 'STANFORD');
IF NOT EXISTS (SELECT 1 FROM RAP.university WHERE university_code = 'MIT')
    INSERT INTO RAP.university (university_name, university_code) VALUES ('MIT', 'MIT');
IF NOT EXISTS (SELECT 1 FROM RAP.university WHERE university_code = 'OXFORD')
    INSERT INTO RAP.university (university_name, university_code) VALUES ('Oxford University', 'OXFORD');
IF NOT EXISTS (SELECT 1 FROM RAP.university WHERE university_code = 'CAMBRIDGE')
    INSERT INTO RAP.university (university_name, university_code) VALUES ('Cambridge University', 'CAMBRIDGE');
IF NOT EXISTS (SELECT 1 FROM RAP.university WHERE university_code = 'YALE')
    INSERT INTO RAP.university (university_name, university_code) VALUES ('Yale University', 'YALE');
IF NOT EXISTS (SELECT 1 FROM RAP.university WHERE university_code = 'PRINCETON')
    INSERT INTO RAP.university (university_name, university_code) VALUES ('Princeton University', 'PRINCETON');
IF NOT EXISTS (SELECT 1 FROM RAP.university WHERE university_code = 'COLUMBIA')
    INSERT INTO RAP.university (university_name, university_code) VALUES ('Columbia University', 'COLUMBIA');
IF NOT EXISTS (SELECT 1 FROM RAP.university WHERE university_code = 'UCHICAGO')
    INSERT INTO RAP.university (university_name, university_code) VALUES ('University of Chicago', 'UCHICAGO');
IF NOT EXISTS (SELECT 1 FROM RAP.university WHERE university_code = 'IMPERIAL')
    INSERT INTO RAP.university (university_name, university_code) VALUES ('Imperial College London', 'IMPERIAL');


-- ===================================================================
-- SEED DATA - Default Roles (idempotent)
-- ===================================================================
IF NOT EXISTS (SELECT 1 FROM RAP.ROLE_REF WHERE role_name = 'USER')
    INSERT INTO RAP.ROLE_REF (role_name, description) VALUES ('USER', 'Internal user with read access');
IF NOT EXISTS (SELECT 1 FROM RAP.ROLE_REF WHERE role_name = 'EXTERNAL_USER')
    INSERT INTO RAP.ROLE_REF (role_name, description) VALUES ('EXTERNAL_USER', 'External user with read access');
IF NOT EXISTS (SELECT 1 FROM RAP.ROLE_REF WHERE role_name = 'MANAGER')
    INSERT INTO RAP.ROLE_REF (role_name, description) VALUES ('MANAGER', 'Manager with read/write access to managed entities');
IF NOT EXISTS (SELECT 1 FROM RAP.ROLE_REF WHERE role_name = 'ADMIN')
    INSERT INTO RAP.ROLE_REF (role_name, description) VALUES ('ADMIN', 'System administrator with full access');
IF NOT EXISTS (SELECT 1 FROM RAP.ROLE_REF WHERE role_name = 'INTERNAL_USER')
    INSERT INTO RAP.ROLE_REF (role_name, description) VALUES ('INTERNAL_USER', 'Internal user with access to internal dashboard and university-scoped data');
IF NOT EXISTS (SELECT 1 FROM RAP.ROLE_REF WHERE role_name = 'task_admin')
    INSERT INTO RAP.ROLE_REF (role_name, description) VALUES ('task_admin', 'Task Administrator');
PRINT 'Seeded RAP.ROLE_REF';
GO

-- JBPM roles (matches V12 seed data)
IF NOT EXISTS (SELECT 1 FROM RAP.ROLE_REF WHERE role_name = 'kie-server')
    INSERT INTO RAP.ROLE_REF (role_name, description) VALUES ('kie-server', 'kie-server');
IF NOT EXISTS (SELECT 1 FROM RAP.ROLE_REF WHERE role_name = 'admin')
    INSERT INTO RAP.ROLE_REF (role_name, description) VALUES ('admin', 'admin');
IF NOT EXISTS (SELECT 1 FROM RAP.ROLE_REF WHERE role_name = 'user')
    INSERT INTO RAP.ROLE_REF (role_name, description) VALUES ('user', 'user');
PRINT 'Seeded RAP.ROLE_REF';
GO

-- Default JBPM service user (kieserver) - matches V12 seed data
IF NOT EXISTS (SELECT 1 FROM RAP.USER_INFO WHERE email = 'kieserver')
BEGIN
    INSERT INTO RAP.USER_INFO (email, pwd, lang, oidc_subject) VALUES ('kieserver', 'kieserver123', 'en-UK', '1234567890A');

    DECLARE @kieUserId INT = SCOPE_IDENTITY();

    INSERT INTO RAP.USER_ROLE (user_id, role_id)
    SELECT @kieUserId, r.id FROM RAP.ROLE_REF r WHERE r.role_name = 'kie-server';

    INSERT INTO RAP.USER_ROLE (user_id, role_id)
    SELECT @kieUserId, r.id FROM RAP.ROLE_REF r WHERE r.role_name = 'admin';

    INSERT INTO RAP.USER_ROLE (user_id, role_id)
    SELECT @kieUserId, r.id FROM RAP.ROLE_REF r WHERE r.role_name = 'user';

    /*INSERT INTO JBPM.USER_GROUP (user_id, group_id, role_id)
    SELECT @kieUserId, 'Administrators', r.id FROM JBPM.ROLE_REF r WHERE r.role_code = 'admin';*/

    PRINT 'Seeded JBPM kieserver user with roles and groups';
END
ELSE
    PRINT 'JBPM kieserver user already exists';
GO

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

-- 1. Insert user arif.mohammed@nexgeninc.com (if not already present)
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
    INSERT INTO RAP.USER_ROLE (user_id, role_id, granted_by, university_id)
    SELECT 
       u.id, r.id, 'flyway-migration-V9', univ.ID AS UNIVID
    FROM RAP.USER_INFO u
    CROSS JOIN RAP.ROLE_REF r
    CROSS JOIN RAP.UNIVERSITY univ
    WHERE u.email = 'arif.mohammed@nexgeninc.com'
      AND r.role_name = 'INTERNAL_USER'
    PRINT 'Assigned INTERNAL_USER role to arif.mohammed@nexgeninc.com and mapped to ALL universities';
END
ELSE
BEGIN
    PRINT 'INTERNAL_USER role already assigned to arif.mohammed@nexgeninc.com';
END

-- 1. Insert user wbadmin(if not already present)
IF NOT EXISTS (SELECT 1 FROM RAP.USER_INFO WHERE email = 'wbadmin')
BEGIN
    INSERT INTO RAP.USER_INFO (oidc_subject, email, full_name, is_active)
    VALUES (
        'aad|wbadmin',   -- synthetic OIDC subject (updated on first real login)
        'wbadmin',
        'WB Admin',
        1
    );
    PRINT 'Inserted JBPM admin user wbadmin';
END
ELSE
BEGIN
    PRINT 'JBPM admin user wbadmin already exists';
END

-- 2. Assign task_admin role (if not already assigned)
IF NOT EXISTS (
    SELECT 1
    FROM RAP.USER_ROLE ur
    INNER JOIN RAP.USER_INFO u  ON ur.user_id = u.id
    INNER JOIN RAP.ROLE_REF r   ON ur.role_id = r.id
    WHERE u.email = 'wbadmin'
      AND r.role_name = 'task_admin'
)
BEGIN
    INSERT INTO RAP.USER_ROLE (user_id, role_id, granted_by)
    SELECT 
       u.id, r.id, 'flyway-migration-V9'
    FROM RAP.USER_INFO u
    CROSS JOIN RAP.ROLE_REF r
    WHERE u.email = 'wbadmin'
      AND r.role_name = 'task_admin'
    PRINT 'Assigned task_admin role to wbadmin';
END
ELSE
BEGIN
    PRINT 'task_admin role already assigned to wbadmin';
END

