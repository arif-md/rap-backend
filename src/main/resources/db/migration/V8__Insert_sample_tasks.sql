-- Insert sample tasks for testing
-- Note: Tasks are created without assigned_to initially
-- After login, you can assign them using: UPDATE task SET assigned_to = 'YOUR-USER-UUID-HERE'

-- Sample tasks
INSERT INTO RAP.task ([function], task, application_number, application_name, issuing_office, type, status, assigned_to, due_date, created_by, updated_by)
VALUES 
('Review', 'Review application documents', 'APP-2025-001', 'Building Permit Application', 'Planning Department', 'APPROVAL', 'PENDING', NULL, DATEADD(DAY, 5, GETDATE()), 'system', 'system'),
('Inspection', 'Conduct site inspection', 'APP-2025-002', 'Environmental Clearance', 'Environmental Office', 'INSPECTION', 'IN_PROGRESS', NULL, DATEADD(DAY, 3, GETDATE()), 'system', 'system'),
('Verification', 'Verify submitted documents', 'APP-2025-003', 'License Renewal', 'Licensing Department', 'VERIFICATION', 'PENDING', NULL, DATEADD(DAY, 7, GETDATE()), 'system', 'system'),
('Approval', 'Final approval required', 'APP-2025-004', 'Construction Permit', 'Building Department', 'APPROVAL', 'PENDING', NULL, DATEADD(DAY, 2, GETDATE()), 'system', 'system'),
('Assessment', 'Assess compliance requirements', 'APP-2025-005', 'Safety Certification', 'Safety Office', 'ASSESSMENT', 'PENDING', NULL, DATEADD(DAY, 10, GETDATE()), 'system', 'system');

-- To assign tasks to yourself after login, find your user ID and run:
-- SELECT id, email FROM [user];
-- UPDATE RAP.task SET assigned_to = 'YOUR-USER-UUID-HERE' WHERE assigned_to IS NULL;
