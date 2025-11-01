-- Create the application database if it doesn't exist
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'appdb')
BEGIN
    CREATE DATABASE appdb;
    PRINT 'Database appdb created successfully';
END
ELSE
BEGIN
    PRINT 'Database appdb already exists';
END
GO

USE appdb;
GO

-- You can add your table creation scripts here
-- Example:
-- CREATE TABLE users (
--     id INT PRIMARY KEY IDENTITY(1,1),
--     username NVARCHAR(50) NOT NULL,
--     email NVARCHAR(100) NOT NULL,
--     created_at DATETIME DEFAULT GETDATE()
-- );
