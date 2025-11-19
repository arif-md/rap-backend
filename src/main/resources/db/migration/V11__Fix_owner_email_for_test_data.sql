-- =============================================================================
-- Flyway Migration V12: Fix owner_email for Test Data
-- =============================================================================
-- V11 updated created_by but the /my endpoints filter by owner_email.
-- This migration fixes owner_email to match the test user.
-- =============================================================================

-- Update Applications - the /my endpoint filters WHERE owner_email = user
UPDATE application 
SET owner_email = 'user@raptor.local',
    owner_name = 'Test User'
WHERE owner_email NOT IN ('user@raptor.local');

-- Update Tasks - assuming similar filter
UPDATE task 
SET created_by = 'user@raptor.local', 
    updated_by = 'user@raptor.local'
WHERE created_by = 'system';

-- Update Permits - assuming similar filter  
UPDATE permit 
SET created_by = 'user@raptor.local', 
    updated_by = 'user@raptor.local'
WHERE created_by = 'system';
