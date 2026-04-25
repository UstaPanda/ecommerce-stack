-- Fix the admin password hash if it was seeded with the incorrect value
UPDATE users 
SET password_hash = '$2b$10$ipQJmLIkNlsn7jiuETcjDOiWZI831wKFmGMOliISV/rO.DfiiqX1y'
WHERE email = 'admin@example.com';
