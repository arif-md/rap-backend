-- =============================================================================
-- Flyway Migration V7: Create UNIVERSITY table and add university_id to 
-- application and permit tables
-- =============================================================================

-- University reference table
CREATE TABLE RAP.university (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    university_name NVARCHAR(255) NOT NULL,
    university_code NVARCHAR(50) NOT NULL UNIQUE,
    status NVARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),

    INDEX idx_university_name (university_name),
    INDEX idx_university_code (university_code),
    INDEX idx_university_status (status)
);

-- Seed universities
INSERT INTO RAP.university (university_name, university_code) VALUES
    ('Harvard University',          'HARVARD'),
    ('Stanford University',         'STANFORD'),
    ('MIT',                         'MIT'),
    ('Oxford University',           'OXFORD'),
    ('Cambridge University',        'CAMBRIDGE'),
    ('Yale University',             'YALE'),
    ('Princeton University',        'PRINCETON'),
    ('Columbia University',         'COLUMBIA'),
    ('University of Chicago',       'UCHICAGO'),
    ('Imperial College London',     'IMPERIAL');

-- Add university_id column to application table (nullable for existing rows)
ALTER TABLE RAP.application ADD university_id BIGINT NULL;

-- Add foreign key
ALTER TABLE RAP.application
ADD CONSTRAINT fk_application_university
FOREIGN KEY (university_id) REFERENCES RAP.university(id);

-- Add index for university_id lookups
CREATE INDEX idx_application_university_id ON RAP.application(university_id);

-- Add university_id column to permit table (nullable for existing rows)
ALTER TABLE RAP.permit ADD university_id BIGINT NULL;

-- Add foreign key
ALTER TABLE RAP.permit
ADD CONSTRAINT fk_permit_university
FOREIGN KEY (university_id) REFERENCES RAP.university(id);

-- Add index for university_id lookups
CREATE INDEX idx_permit_university_id ON RAP.permit(university_id);
