-- Create the application database if it doesn't exist
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'raptordb')
BEGIN
    CREATE DATABASE raptordb;
    PRINT 'Database raptordb created successfully';
END
ELSE
BEGIN
    PRINT 'Database raptordb already exists';
END
GO

USE raptordb;
GO

-- You can add your table creation scripts here
-- Example:
-- CREATE TABLE users (
--     id INT PRIMARY KEY IDENTITY(1,1),
--     username NVARCHAR(50) NOT NULL,
--     email NVARCHAR(100) NOT NULL,
--     created_at DATETIME DEFAULT GETDATE()
-- );
