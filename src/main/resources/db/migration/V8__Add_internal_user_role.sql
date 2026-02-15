-- =============================================================================
-- Flyway Migration V8: Add INTERNAL_USER role to ROLE_REF table
-- =============================================================================
-- The AzureAdOidcUserService assigns ROLE_INTERNAL_USER to users who log in
-- via Azure AD SSO or keycloak-internal provider. The role name (without ROLE_
-- prefix) must exist in ROLE_REF for syncRolesToDatabase to persist it.
-- =============================================================================

-- Add INTERNAL_USER role if it doesn't already exist
IF NOT EXISTS (SELECT 1 FROM RAP.ROLE_REF WHERE role_name = 'INTERNAL_USER')
BEGIN
    INSERT INTO RAP.ROLE_REF (role_name, description)
    VALUES ('INTERNAL_USER', 'Internal user with access to internal dashboard and university-scoped data');
    PRINT 'Added INTERNAL_USER role to ROLE_REF';
END
ELSE
BEGIN
    PRINT 'INTERNAL_USER role already exists in ROLE_REF';
END
