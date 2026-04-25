-- V19: Fix Store Ownership and Role Consistency
-- This migration ensures the corporate user correctly owns the second store
-- and that all roles are properly set for the manager users.

-- 1. Ensure corporate@example.com has the CORPORATE role
UPDATE users SET role_type = 'CORPORATE' WHERE email = 'corporate@example.com';

-- 2. Explicitly set Elite Tech Solutions ownership to the corporate user
UPDATE stores 
SET owner_id = (SELECT id FROM users WHERE email = 'corporate@example.com')
WHERE name = 'Elite Tech Solutions';

-- 3. Safety Check: If the corporate user was somehow deleted or failed to create, recreate it
INSERT INTO users (
    email, 
    name, 
    password_hash, 
    role_type, 
    provider, 
    is_verified, 
    two_factor_enabled, 
    failed_login_attempts,
    created_at
) 
SELECT 
    'corporate@example.com', 
    'Elite Tech Store Manager', 
    '$2b$10$RdVDouPi1pjs0QdjxTHo7OGy/6Y.liMGy/Zw5I7TpmakYP9BvuIYq', 
    'CORPORATE', 
    'LOCAL', 
    true, 
    false, 
    0,
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'corporate@example.com'
);

-- 4. Re-verify the store exists and has the correct owner
INSERT INTO stores (
    owner_id, 
    name, 
    description, 
    status, 
    created_at
)
SELECT 
    id, 
    'Elite Tech Solutions', 
    'Premium gadgets and accessories for tech enthusiasts.', 
    'OPEN', 
    NOW()
FROM users 
WHERE email = 'corporate@example.com'
AND NOT EXISTS (
    SELECT 1 FROM stores WHERE name = 'Elite Tech Solutions'
);
