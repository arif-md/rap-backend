-- Create task table for workflow tasks
CREATE TABLE RAP.task (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    [function] NVARCHAR(255) NOT NULL,
    task NVARCHAR(500) NOT NULL,
    application_number NVARCHAR(50),
    application_name NVARCHAR(255),
    issuing_office NVARCHAR(255),
    type NVARCHAR(50) NOT NULL,
    status NVARCHAR(50) NOT NULL DEFAULT 'PENDING',
    assigned_to UNIQUEIDENTIFIER,
    due_date DATETIME2,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    created_by NVARCHAR(255) NOT NULL,
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_by NVARCHAR(255) NOT NULL
);

-- Create index for assigned_to for faster queries
CREATE INDEX IDX_task_assigned_to ON RAP.task(assigned_to);

-- Create index for status for faster filtering
CREATE INDEX IDX_task_status ON RAP.task(status);

-- Create index for due_date for sorting
CREATE INDEX IDX_task_due_date ON RAP.task(due_date);
