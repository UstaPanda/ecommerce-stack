-- V12: Add a second corporate user and a second store
-- Corporate Password is: corp123

-- 1. Create Corporate User
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

-- 2. Create Second Store owned by the new Corporate User
INSERT INTO stores (
    owner_id, 
    name, 
    description, 
    status, 
    created_at, 
    updated_at
)
SELECT 
    id, 
    'Elite Tech Solutions', 
    'Premium gadgets and accessories for tech enthusiasts.', 
    'ACTIVE', 
    NOW(), 
    NOW()
FROM users 
WHERE email = 'corporate@example.com'
AND NOT EXISTS (
    SELECT 1 FROM stores WHERE name = 'Elite Tech Solutions'
);

-- 3. Move some products to the new store (IDs 17, 27, 28, 29)
UPDATE products 
SET store_id = (SELECT id FROM stores WHERE name = 'Elite Tech Solutions')
WHERE id IN (17, 27, 28, 29);
