-- Fix test data to use user@raptor.local instead of 'system'
-- This allows the /my endpoints to return test data for the logged-in user

UPDATE application 
SET created_by = 'user@raptor.local', 
    updated_by = 'user@raptor.local' 
WHERE created_by = 'system';

UPDATE task 
SET created_by = 'user@raptor.local', 
    updated_by = 'user@raptor.local' 
WHERE created_by = 'system';

UPDATE permit 
SET created_by = 'user@raptor.local', 
    updated_by = 'user@raptor.local' 
WHERE created_by = 'system';

-- Verify counts
SELECT 'Applications' AS entity, COUNT(*) AS total_count, 
       SUM(CASE WHEN created_by = 'user@raptor.local' THEN 1 ELSE 0 END) AS user_owned
FROM application
UNION ALL
SELECT 'Tasks', COUNT(*), SUM(CASE WHEN created_by = 'user@raptor.local' THEN 1 ELSE 0 END)
FROM task
UNION ALL  
SELECT 'Permits', COUNT(*), SUM(CASE WHEN created_by = 'user@raptor.local' THEN 1 ELSE 0 END)
FROM permit;
