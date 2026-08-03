-- =============================================================================
-- Flyway Migration V11: Drop RAP.permit
-- =============================================================================
-- RAP.permit (created in V4) backed a standalone Permit CRUD stack (Permit.java,
-- PermitService, PermitHandler, PermitMapper) that was never wired to the frontend -
-- the "My Permits"/"Permits" dashboard tabs were always backed by RAP.application
-- filtered to ACCEPTED status instead (see ApplicationService.getAcceptedApplicationsBy*,
-- now exposed as ApplicationController's /my/admissions and /university/{id}/admissions).
-- Dropping this unused table and its Permit domain stack as part of renaming the
-- "permit" concept to "admission" throughout the app.
-- =============================================================================
IF EXISTS (SELECT 1 FROM sys.tables t JOIN sys.schemas s ON t.schema_id = s.schema_id WHERE s.name = 'RAP' AND t.name = 'permit')
BEGIN
    DROP TABLE RAP.permit;
    PRINT 'Dropped table: RAP.permit';
END
ELSE
    PRINT 'Table RAP.permit does not exist';
GO
