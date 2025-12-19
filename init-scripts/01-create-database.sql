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
