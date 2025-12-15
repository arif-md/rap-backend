-- ================================================================
-- V12: Assign default USER role to users without roles
-- ================================================================
-- This migration ensures all users have at least the USER role assigned.
-- Needed because role assignment during user creation may fail due to
-- transaction boundaries or other transient issues.

-- Assign USER role to all users who don't have any roles
INSERT INTO RAP.user_roles (id, user_id, role_id, granted_by)
SELECT 
    NEWID() AS id,
    u.id AS user_id,
    (SELECT id FROM RAP.roles WHERE role_name = 'USER') AS role_id,
    'MIGRATION_V12' AS granted_by
FROM RAP.users u
WHERE NOT EXISTS (
    SELECT 1 FROM RAP.user_roles ur WHERE ur.user_id = u.id
);

-- Display count of users who received the default role
SELECT 
    COUNT(*) AS users_assigned_default_role
FROM RAP.users u
WHERE NOT EXISTS (
    SELECT 1 FROM RAP.user_roles ur WHERE ur.user_id = u.id
);
