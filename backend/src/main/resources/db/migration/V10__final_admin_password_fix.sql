-- Final fix for admin password to ensure login works
-- This overrides any previous corrupted hashes
UPDATE users 
SET password_hash = '$2b$10$ipQJmLIkNlsn7jiuETcjDOiWZI831wKFmGMOliISV/rO.DfiiqX1y'
WHERE email = 'admin@example.com';
