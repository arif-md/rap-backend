-- Create permit table
CREATE TABLE RAP.permit (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    permit_number NVARCHAR(50) NOT NULL UNIQUE,
    permit_type NVARCHAR(100) NOT NULL,
    status NVARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    issue_date DATE NOT NULL,
    expiry_date DATE,
    holder_id BIGINT NOT NULL,
    description NVARCHAR(1000),
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    created_by NVARCHAR(255) NOT NULL,
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_by NVARCHAR(255) NOT NULL
);

-- Create index for holder_id for faster queries
CREATE INDEX IDX_permit_holder_id ON RAP.permit(holder_id);

-- Create index for permit_number for faster lookups
CREATE INDEX IDX_permit_number ON RAP.permit(permit_number);

-- Create index for status for filtering
CREATE INDEX IDX_permit_status ON RAP.permit(status);

-- Create index for expiry_date for filtering expiring permits
CREATE INDEX IDX_permit_expiry_date ON RAP.permit(expiry_date);
