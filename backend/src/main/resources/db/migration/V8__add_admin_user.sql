-- Add a new Admin user during deployment
-- Password is: admin123
INSERT INTO users (
    email, 
    name, 
    password_hash, 
    role_type, 
    provider, 
    is_verified, 
    two_factor_enabled, 
    failed_login_attempts
) 
SELECT 
    'admin@example.com', 
    'System Admin', 
    '$2b$10$ipQJmLIkNlsn7jiuETcjDOiWZI831wKFmGMOliISV/rO.DfiiqX1y', 
    'ADMIN', 
    'LOCAL', 
    true, 
    false, 
    0
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'admin@example.com'
);
