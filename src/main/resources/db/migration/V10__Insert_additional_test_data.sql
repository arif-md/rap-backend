-- =============================================================================
-- Flyway Migration V10: Insert Additional Test Data for Pagination Testing
-- =============================================================================
-- This migration inserts sufficient test data for Applications, Permits, and Tasks
-- to enable proper pagination testing (25+ records per entity)
-- =============================================================================

-- =====================
-- 1. APPLICATION DATA
-- =====================
-- Insert 30 application records for pagination testing

INSERT INTO application (application_name, application_code, description, status, owner_name, owner_email, created_by, updated_by)
VALUES 
-- Active Applications (20 records)
('Enterprise Resource Planning', 'ERP-2025-001', 'Comprehensive ERP system for business management', 'ACTIVE', 'John Smith', 'john.smith@company.com', 'system', 'system'),
('Customer Relationship Management', 'CRM-2025-002', 'CRM platform for customer engagement', 'ACTIVE', 'Sarah Johnson', 'sarah.j@company.com', 'system', 'system'),
('Human Resources Management', 'HRM-2025-003', 'HR management and payroll system', 'ACTIVE', 'Michael Chen', 'michael.chen@company.com', 'system', 'system'),
('Supply Chain Management', 'SCM-2025-004', 'End-to-end supply chain visibility platform', 'ACTIVE', 'Emily Davis', 'emily.davis@company.com', 'system', 'system'),
('Business Intelligence Dashboard', 'BID-2025-005', 'Real-time analytics and reporting dashboard', 'ACTIVE', 'Robert Wilson', 'robert.w@company.com', 'system', 'system'),
('E-Commerce Platform', 'ECP-2025-006', 'Online retail and marketplace platform', 'ACTIVE', 'Jennifer Lee', 'jennifer.lee@company.com', 'system', 'system'),
('Mobile Banking Application', 'MBA-2025-007', 'Secure mobile banking for retail customers', 'ACTIVE', 'David Martinez', 'david.m@company.com', 'system', 'system'),
('Inventory Management System', 'IMS-2025-008', 'Warehouse and inventory tracking system', 'ACTIVE', 'Lisa Anderson', 'lisa.anderson@company.com', 'system', 'system'),
('Learning Management System', 'LMS-2025-009', 'Corporate training and e-learning platform', 'ACTIVE', 'Kevin Brown', 'kevin.brown@company.com', 'system', 'system'),
('Project Management Tool', 'PMT-2025-010', 'Agile project planning and collaboration tool', 'ACTIVE', 'Amanda Taylor', 'amanda.taylor@company.com', 'system', 'system'),
('Document Management System', 'DMS-2025-011', 'Enterprise document storage and workflow', 'ACTIVE', 'Christopher White', 'chris.white@company.com', 'system', 'system'),
('Customer Support Portal', 'CSP-2025-012', 'Self-service customer support and ticketing', 'ACTIVE', 'Michelle Garcia', 'michelle.g@company.com', 'system', 'system'),
('Financial Reporting System', 'FRS-2025-013', 'Automated financial reports and compliance', 'ACTIVE', 'James Rodriguez', 'james.rod@company.com', 'system', 'system'),
('Quality Assurance Platform', 'QAP-2025-014', 'Quality control and testing management', 'ACTIVE', 'Patricia Martinez', 'patricia.m@company.com', 'system', 'system'),
('Asset Management System', 'AMS-2025-015', 'Fixed asset tracking and depreciation', 'ACTIVE', 'Daniel Hernandez', 'daniel.h@company.com', 'system', 'system'),
('Procurement Platform', 'PRP-2025-016', 'Vendor management and purchase orders', 'ACTIVE', 'Jessica Lopez', 'jessica.lopez@company.com', 'system', 'system'),
('Fleet Management System', 'FMS-2025-017', 'Vehicle tracking and maintenance scheduling', 'ACTIVE', 'Matthew Wilson', 'matthew.w@company.com', 'system', 'system'),
('Time Tracking Application', 'TTA-2025-018', 'Employee time and attendance management', 'ACTIVE', 'Ashley Thompson', 'ashley.t@company.com', 'system', 'system'),
('Contract Management System', 'CMS-2025-019', 'Legal contract lifecycle management', 'ACTIVE', 'Andrew Clark', 'andrew.clark@company.com', 'system', 'system'),
('Risk Management Platform', 'RMP-2025-020', 'Enterprise risk assessment and mitigation', 'ACTIVE', 'Stephanie Lewis', 'stephanie.l@company.com', 'system', 'system'),

-- Pending Applications (5 records)
('Payment Gateway Integration', 'PGI-2025-021', 'Third-party payment processing integration', 'PENDING', 'Brian Walker', 'brian.walker@company.com', 'system', 'system'),
('Customer Feedback System', 'CFS-2025-022', 'Multi-channel customer feedback collection', 'PENDING', 'Nicole Hall', 'nicole.hall@company.com', 'system', 'system'),
('Compliance Monitoring Tool', 'CMT-2025-023', 'Regulatory compliance tracking system', 'PENDING', 'Ryan Allen', 'ryan.allen@company.com', 'system', 'system'),
('Sales Force Automation', 'SFA-2025-024', 'Sales pipeline and opportunity management', 'PENDING', 'Lauren Young', 'lauren.young@company.com', 'system', 'system'),
('Data Analytics Engine', 'DAE-2025-025', 'Advanced data mining and predictive analytics', 'PENDING', 'Justin King', 'justin.king@company.com', 'system', 'system'),

-- Inactive Applications (3 records)
('Legacy Accounting System', 'LAS-2024-026', 'Deprecated accounting system - migrating to ERP', 'INACTIVE', 'Mark Wright', 'mark.wright@company.com', 'system', 'system'),
('Old Expense Tracker', 'OET-2024-027', 'Replaced by integrated expense module', 'INACTIVE', 'Karen Scott', 'karen.scott@company.com', 'system', 'system'),
('Basic Inventory Tool', 'BIT-2024-028', 'Superseded by modern IMS', 'INACTIVE', 'Eric Green', 'eric.green@company.com', 'system', 'system'),

-- Archived Applications (2 records)
('Pilot CRM System', 'PCS-2023-029', 'Initial CRM pilot project - archived', 'ARCHIVED', 'Melissa Adams', 'melissa.adams@company.com', 'system', 'system'),
('Test Workflow Engine', 'TWE-2023-030', 'Proof of concept workflow automation', 'ARCHIVED', 'Gregory Baker', 'gregory.baker@company.com', 'system', 'system');


-- =====================
-- 2. WORKFLOW TASKS
-- =====================
-- Insert 35 additional task records (total with V8 will be 40+)

INSERT INTO task ([function], task, application_number, application_name, issuing_office, type, status, assigned_to, due_date, created_by, updated_by)
VALUES 
-- Pending Tasks (15 records)
('Review', 'Review license application documents', 'APP-2025-006', 'Business License Application', 'Licensing Department', 'APPROVAL', 'PENDING', NULL, DATEADD(DAY, 8, GETDATE()), 'system', 'system'),
('Verification', 'Verify proof of insurance', 'APP-2025-007', 'Insurance Verification', 'Risk Management', 'VERIFICATION', 'PENDING', NULL, DATEADD(DAY, 4, GETDATE()), 'system', 'system'),
('Assessment', 'Assess tax compliance status', 'APP-2025-008', 'Tax Clearance Certificate', 'Revenue Office', 'ASSESSMENT', 'PENDING', NULL, DATEADD(DAY, 12, GETDATE()), 'system', 'system'),
('Review', 'Review zoning variance request', 'APP-2025-009', 'Zoning Variance Application', 'Zoning Board', 'APPROVAL', 'PENDING', NULL, DATEADD(DAY, 15, GETDATE()), 'system', 'system'),
('Inspection', 'Conduct fire safety inspection', 'APP-2025-010', 'Fire Safety Clearance', 'Fire Department', 'INSPECTION', 'PENDING', NULL, DATEADD(DAY, 6, GETDATE()), 'system', 'system'),
('Verification', 'Verify property ownership documents', 'APP-2025-011', 'Property Transfer Approval', 'Land Registry', 'VERIFICATION', 'PENDING', NULL, DATEADD(DAY, 9, GETDATE()), 'system', 'system'),
('Approval', 'Final approval for operating license', 'APP-2025-012', 'Operating License Renewal', 'Business Regulation', 'APPROVAL', 'PENDING', NULL, DATEADD(DAY, 3, GETDATE()), 'system', 'system'),
('Assessment', 'Assess environmental impact', 'APP-2025-013', 'Environmental Impact Assessment', 'Environmental Agency', 'ASSESSMENT', 'PENDING', NULL, DATEADD(DAY, 20, GETDATE()), 'system', 'system'),
('Review', 'Review health permit application', 'APP-2025-014', 'Health Permit Application', 'Health Department', 'APPROVAL', 'PENDING', NULL, DATEADD(DAY, 7, GETDATE()), 'system', 'system'),
('Verification', 'Verify building plans compliance', 'APP-2025-015', 'Building Plans Verification', 'Building Department', 'VERIFICATION', 'PENDING', NULL, DATEADD(DAY, 11, GETDATE()), 'system', 'system'),
('Inspection', 'Conduct electrical safety inspection', 'APP-2025-016', 'Electrical Safety Clearance', 'Electrical Safety Board', 'INSPECTION', 'PENDING', NULL, DATEADD(DAY, 5, GETDATE()), 'system', 'system'),
('Review', 'Review signage permit request', 'APP-2025-017', 'Signage Permit Application', 'Urban Planning', 'APPROVAL', 'PENDING', NULL, DATEADD(DAY, 14, GETDATE()), 'system', 'system'),
('Assessment', 'Assess noise pollution compliance', 'APP-2025-018', 'Noise Compliance Certificate', 'Environmental Office', 'ASSESSMENT', 'PENDING', NULL, DATEADD(DAY, 10, GETDATE()), 'system', 'system'),
('Verification', 'Verify financial statements', 'APP-2025-019', 'Financial Audit Clearance', 'Auditing Department', 'VERIFICATION', 'PENDING', NULL, DATEADD(DAY, 18, GETDATE()), 'system', 'system'),
('Approval', 'Final approval for demolition permit', 'APP-2025-020', 'Demolition Permit Application', 'Building Department', 'APPROVAL', 'PENDING', NULL, DATEADD(DAY, 13, GETDATE()), 'system', 'system'),

-- In Progress Tasks (10 records)
('Inspection', 'Conduct plumbing inspection', 'APP-2025-021', 'Plumbing Compliance Check', 'Building Inspector', 'INSPECTION', 'IN_PROGRESS', NULL, DATEADD(DAY, 4, GETDATE()), 'system', 'system'),
('Review', 'Review liquor license application', 'APP-2025-022', 'Liquor License Application', 'Alcohol Control Board', 'APPROVAL', 'IN_PROGRESS', NULL, DATEADD(DAY, 6, GETDATE()), 'system', 'system'),
('Assessment', 'Assess traffic impact study', 'APP-2025-023', 'Traffic Impact Assessment', 'Transportation Department', 'ASSESSMENT', 'IN_PROGRESS', NULL, DATEADD(DAY, 16, GETDATE()), 'system', 'system'),
('Verification', 'Verify professional credentials', 'APP-2025-024', 'Professional License Verification', 'Professional Board', 'VERIFICATION', 'IN_PROGRESS', NULL, DATEADD(DAY, 8, GETDATE()), 'system', 'system'),
('Inspection', 'Conduct structural inspection', 'APP-2025-025', 'Structural Safety Inspection', 'Civil Engineering Dept', 'INSPECTION', 'IN_PROGRESS', NULL, DATEADD(DAY, 7, GETDATE()), 'system', 'system'),
('Review', 'Review parking plan submission', 'APP-2025-026', 'Parking Plan Approval', 'Transportation Planning', 'APPROVAL', 'IN_PROGRESS', NULL, DATEADD(DAY, 9, GETDATE()), 'system', 'system'),
('Assessment', 'Assess water usage compliance', 'APP-2025-027', 'Water Usage Permit', 'Water Resources', 'ASSESSMENT', 'IN_PROGRESS', NULL, DATEADD(DAY, 12, GETDATE()), 'system', 'system'),
('Verification', 'Verify hazardous materials handling', 'APP-2025-028', 'Hazmat Storage Permit', 'Fire Safety Department', 'VERIFICATION', 'IN_PROGRESS', NULL, DATEADD(DAY, 10, GETDATE()), 'system', 'system'),
('Inspection', 'Conduct accessibility inspection', 'APP-2025-029', 'ADA Compliance Inspection', 'Accessibility Board', 'INSPECTION', 'IN_PROGRESS', NULL, DATEADD(DAY, 5, GETDATE()), 'system', 'system'),
('Review', 'Review waste management plan', 'APP-2025-030', 'Waste Management Approval', 'Sanitation Department', 'APPROVAL', 'IN_PROGRESS', NULL, DATEADD(DAY, 11, GETDATE()), 'system', 'system'),

-- Completed Tasks (5 records)
('Review', 'Review completed successfully', 'APP-2025-031', 'Completed Building Permit', 'Planning Department', 'APPROVAL', 'COMPLETED', NULL, DATEADD(DAY, -5, GETDATE()), 'system', 'system'),
('Inspection', 'Inspection passed', 'APP-2025-032', 'Completed Safety Inspection', 'Safety Office', 'INSPECTION', 'COMPLETED', NULL, DATEADD(DAY, -8, GETDATE()), 'system', 'system'),
('Verification', 'Documents verified and approved', 'APP-2025-033', 'Completed Document Verification', 'Records Office', 'VERIFICATION', 'COMPLETED', NULL, DATEADD(DAY, -3, GETDATE()), 'system', 'system'),
('Assessment', 'Assessment completed favorably', 'APP-2025-034', 'Completed Impact Assessment', 'Environmental Office', 'ASSESSMENT', 'COMPLETED', NULL, DATEADD(DAY, -12, GETDATE()), 'system', 'system'),
('Approval', 'Final approval granted', 'APP-2025-035', 'Completed License Approval', 'Licensing Bureau', 'APPROVAL', 'COMPLETED', NULL, DATEADD(DAY, -2, GETDATE()), 'system', 'system'),

-- Rejected Tasks (3 records)
('Review', 'Application rejected - incomplete documentation', 'APP-2025-036', 'Rejected Permit Application', 'Permits Office', 'APPROVAL', 'REJECTED', NULL, DATEADD(DAY, -4, GETDATE()), 'system', 'system'),
('Verification', 'Verification failed - discrepancies found', 'APP-2025-037', 'Failed Verification', 'Compliance Office', 'VERIFICATION', 'REJECTED', NULL, DATEADD(DAY, -6, GETDATE()), 'system', 'system'),
('Inspection', 'Inspection failed - safety violations', 'APP-2025-038', 'Failed Safety Inspection', 'Safety Inspector', 'INSPECTION', 'REJECTED', NULL, DATEADD(DAY, -10, GETDATE()), 'system', 'system'),

-- On Hold Tasks (2 records)
('Review', 'Review on hold pending additional information', 'APP-2025-039', 'On Hold Review', 'Review Board', 'APPROVAL', 'ON_HOLD', NULL, DATEADD(DAY, 30, GETDATE()), 'system', 'system'),
('Assessment', 'Assessment paused for third-party consultation', 'APP-2025-040', 'On Hold Assessment', 'Technical Review', 'ASSESSMENT', 'ON_HOLD', NULL, DATEADD(DAY, 25, GETDATE()), 'system', 'system');


-- =====================
-- 3. PERMIT DATA
-- =====================
-- Insert 30 additional permit records (total with V9 will be 35+)
-- These permits will be assigned to the first user in the system

DECLARE @firstUserId UNIQUEIDENTIFIER;
SET @firstUserId = (SELECT TOP 1 id FROM users ORDER BY created_at);

IF @firstUserId IS NOT NULL
BEGIN
    INSERT INTO permit (permit_number, permit_type, status, issue_date, expiry_date, holder_id, description, created_by, updated_by)
    VALUES 
    -- Active Permits (20 records)
    ('PER-2025-006', 'Construction Permit', 'ACTIVE', DATEADD(MONTH, -2, GETDATE()), DATEADD(YEAR, 1, GETDATE()), @firstUserId, 'Commercial building construction permit', 'system', 'system'),
    ('PER-2025-007', 'Electrical Permit', 'ACTIVE', DATEADD(MONTH, -1, GETDATE()), DATEADD(MONTH, 6, GETDATE()), @firstUserId, 'Electrical installation and wiring permit', 'system', 'system'),
    ('PER-2025-008', 'Plumbing Permit', 'ACTIVE', DATEADD(MONTH, -1, GETDATE()), DATEADD(MONTH, 6, GETDATE()), @firstUserId, 'Plumbing system installation permit', 'system', 'system'),
    ('PER-2025-009', 'Sign Permit', 'ACTIVE', DATEADD(WEEK, -2, GETDATE()), DATEADD(YEAR, 2, GETDATE()), @firstUserId, 'Commercial signage installation permit', 'system', 'system'),
    ('PER-2025-010', 'Demolition Permit', 'ACTIVE', DATEADD(WEEK, -3, GETDATE()), DATEADD(MONTH, 3, GETDATE()), @firstUserId, 'Building demolition permit', 'system', 'system'),
    ('PER-2025-011', 'Excavation Permit', 'ACTIVE', DATEADD(WEEK, -1, GETDATE()), DATEADD(MONTH, 4, GETDATE()), @firstUserId, 'Site excavation and grading permit', 'system', 'system'),
    ('PER-2025-012', 'HVAC Permit', 'ACTIVE', DATEADD(MONTH, -2, GETDATE()), DATEADD(YEAR, 1, GETDATE()), @firstUserId, 'Heating and cooling system permit', 'system', 'system'),
    ('PER-2025-013', 'Food Service Permit', 'ACTIVE', DATEADD(MONTH, -5, GETDATE()), DATEADD(YEAR, 1, GETDATE()), @firstUserId, 'Restaurant food service license', 'system', 'system'),
    ('PER-2025-014', 'Liquor License', 'ACTIVE', DATEADD(MONTH, -8, GETDATE()), DATEADD(YEAR, 2, GETDATE()), @firstUserId, 'Alcoholic beverage sales license', 'system', 'system'),
    ('PER-2025-015', 'Health Permit', 'ACTIVE', DATEADD(MONTH, -4, GETDATE()), DATEADD(YEAR, 1, GETDATE()), @firstUserId, 'Healthcare facility operation permit', 'system', 'system'),
    ('PER-2025-016', 'Fire Safety Permit', 'ACTIVE', DATEADD(MONTH, -3, GETDATE()), DATEADD(YEAR, 1, GETDATE()), @firstUserId, 'Fire alarm and suppression system permit', 'system', 'system'),
    ('PER-2025-017', 'Parking Permit', 'ACTIVE', DATEADD(MONTH, -2, GETDATE()), DATEADD(YEAR, 3, GETDATE()), @firstUserId, 'Commercial parking lot operation permit', 'system', 'system'),
    ('PER-2025-018', 'Waste Management Permit', 'ACTIVE', DATEADD(MONTH, -6, GETDATE()), DATEADD(YEAR, 2, GETDATE()), @firstUserId, 'Waste collection and disposal permit', 'system', 'system'),
    ('PER-2025-019', 'Water Discharge Permit', 'ACTIVE', DATEADD(MONTH, -7, GETDATE()), DATEADD(YEAR, 5, GETDATE()), @firstUserId, 'Industrial water discharge permit', 'system', 'system'),
    ('PER-2025-020', 'Air Quality Permit', 'ACTIVE', DATEADD(MONTH, -9, GETDATE()), DATEADD(YEAR, 3, GETDATE()), @firstUserId, 'Air emissions monitoring permit', 'system', 'system'),
    ('PER-2025-021', 'Zoning Variance', 'ACTIVE', DATEADD(MONTH, -4, GETDATE()), DATEADD(YEAR, 10, GETDATE()), @firstUserId, 'Commercial use zoning variance', 'system', 'system'),
    ('PER-2025-022', 'Home Occupation Permit', 'ACTIVE', DATEADD(MONTH, -2, GETDATE()), DATEADD(YEAR, 2, GETDATE()), @firstUserId, 'Home-based business permit', 'system', 'system'),
    ('PER-2025-023', 'Special Event Permit', 'ACTIVE', DATEADD(WEEK, -2, GETDATE()), DATEADD(WEEK, 4, GETDATE()), @firstUserId, 'Public event hosting permit', 'system', 'system'),
    ('PER-2025-024', 'Street Closure Permit', 'ACTIVE', DATEADD(WEEK, -1, GETDATE()), DATEADD(WEEK, 2, GETDATE()), @firstUserId, 'Temporary street closure permit', 'system', 'system'),
    ('PER-2025-025', 'Tree Removal Permit', 'ACTIVE', DATEADD(MONTH, -1, GETDATE()), DATEADD(MONTH, 6, GETDATE()), @firstUserId, 'Protected tree removal permit', 'system', 'system'),

    -- Pending Permits (5 records)
    ('PER-2025-026', 'Noise Variance', 'PENDING', DATEADD(DAY, -3, GETDATE()), DATEADD(YEAR, 1, GETDATE()), @firstUserId, 'Extended hours noise variance permit', 'system', 'system'),
    ('PER-2025-027', 'Pool Construction Permit', 'PENDING', DATEADD(DAY, -5, GETDATE()), DATEADD(YEAR, 2, GETDATE()), @firstUserId, 'Swimming pool construction permit', 'system', 'system'),
    ('PER-2025-028', 'Fence Installation Permit', 'PENDING', DATEADD(DAY, -2, GETDATE()), DATEADD(YEAR, 5, GETDATE()), @firstUserId, 'Property fence installation permit', 'system', 'system'),
    ('PER-2025-029', 'Sidewalk Repair Permit', 'PENDING', DATEADD(DAY, -4, GETDATE()), DATEADD(MONTH, 3, GETDATE()), @firstUserId, 'Public sidewalk repair permit', 'system', 'system'),
    ('PER-2025-030', 'Driveway Access Permit', 'PENDING', DATEADD(DAY, -1, GETDATE()), DATEADD(YEAR, 3, GETDATE()), @firstUserId, 'Commercial driveway access permit', 'system', 'system'),

    -- Expired Permits (3 records)
    ('PER-2023-031', 'Temporary Use Permit', 'EXPIRED', DATEADD(MONTH, -24, GETDATE()), DATEADD(MONTH, -6, GETDATE()), @firstUserId, 'Temporary commercial use permit - expired', 'system', 'system'),
    ('PER-2023-032', 'Mobile Vendor Permit', 'EXPIRED', DATEADD(MONTH, -20, GETDATE()), DATEADD(MONTH, -8, GETDATE()), @firstUserId, 'Mobile food vendor permit - expired', 'system', 'system'),
    ('PER-2024-033', 'Scaffolding Permit', 'EXPIRED', DATEADD(MONTH, -12, GETDATE()), DATEADD(MONTH, -3, GETDATE()), @firstUserId, 'Temporary scaffolding permit - expired', 'system', 'system'),

    -- Revoked Permits (2 records)
    ('PER-2024-034', 'Conditional Use Permit', 'REVOKED', DATEADD(MONTH, -15, GETDATE()), DATEADD(MONTH, -2, GETDATE()), @firstUserId, 'Conditional use permit - revoked for violations', 'system', 'system'),
    ('PER-2024-035', 'Operating License', 'REVOKED', DATEADD(MONTH, -10, GETDATE()), DATEADD(MONTH, -1, GETDATE()), @firstUserId, 'Business operating license - revoked', 'system', 'system');

    PRINT 'Successfully inserted 30 additional permit records for pagination testing.';
END
ELSE
BEGIN
    PRINT 'WARNING: No users found in database - permits not inserted. Please log in first to create a user.';
END

-- =============================================================================
-- Migration Complete
-- =============================================================================
-- Summary:
-- - 30 Application records inserted (various statuses)
-- - 35 Task records inserted (various statuses and types)
-- - 30 Permit records inserted (assigned to first user, various statuses)
-- 
-- Total records after this migration:
-- - Applications: 30
-- - Tasks: 40 (5 from V8 + 35 from V10)
-- - Permits: 35 (5 from V9 + 30 from V10)
-- 
-- All entities now have sufficient data for pagination testing (10 items per page)
-- =============================================================================
