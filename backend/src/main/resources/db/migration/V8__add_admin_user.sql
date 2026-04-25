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
    '$2a$10$EblZqNptyYvcLm/VwDC9uu6HNH9BKAD8/5OdS5nS6.82T9L8A.E6.', 
    'ADMIN', 
    'LOCAL', 
    true, 
    false, 
    0
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'admin@example.com'
);
