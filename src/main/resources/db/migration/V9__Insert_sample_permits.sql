-- Insert sample permits for testing
-- Note: This script requires at least one user to exist in the users table
-- Permits will be assigned to the first user in the database

DECLARE @firstUserId UNIQUEIDENTIFIER;
SET @firstUserId = (SELECT TOP 1 id FROM RAP.users);

-- Only insert permits if a user exists
IF @firstUserId IS NOT NULL
BEGIN
    INSERT INTO RAP.permit (permit_number, permit_type, status, issue_date, expiry_date, holder_id, description, created_by, updated_by)
    VALUES 
    ('PER-2024-001', 'Building Permit', 'ACTIVE', DATEADD(MONTH, -6, GETDATE()), DATEADD(MONTH, 6, GETDATE()), @firstUserId, 'Residential construction permit for 2-story building', 'system', 'system'),
    ('PER-2024-002', 'Environmental Clearance', 'ACTIVE', DATEADD(MONTH, -3, GETDATE()), DATEADD(YEAR, 1, GETDATE()), @firstUserId, 'Environmental compliance certificate', 'system', 'system'),
    ('PER-2024-003', 'Business License', 'ACTIVE', DATEADD(MONTH, -12, GETDATE()), DATEADD(DAY, 30, GETDATE()), @firstUserId, 'General business license - expires soon', 'system', 'system'),
    ('PER-2023-004', 'Safety Certificate', 'EXPIRED', DATEADD(MONTH, -18, GETDATE()), DATEADD(MONTH, -6, GETDATE()), @firstUserId, 'Workplace safety certification', 'system', 'system'),
    ('PER-2025-005', 'Occupancy Permit', 'ACTIVE', DATEADD(MONTH, -1, GETDATE()), DATEADD(YEAR, 2, GETDATE()), @firstUserId, 'Certificate of occupancy for commercial building', 'system', 'system');
END
ELSE
BEGIN
    PRINT 'No users found in database - permits not inserted. Please log in first to create a user.';
END

-- To reassign permits to a specific user after login:
-- UPDATE permit SET holder_id = 'YOUR-USER-UUID-HERE';
