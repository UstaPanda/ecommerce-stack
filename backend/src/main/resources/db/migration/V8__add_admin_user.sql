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
    '$2a$10$8.UnVuG9HHgffUDAlk8qn.R.p4Z1r1qP9R2X8Z2/M48/28/M48/28', 
    'ADMIN', 
    'LOCAL', 
    true, 
    false, 
    0
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'admin@example.com'
);
