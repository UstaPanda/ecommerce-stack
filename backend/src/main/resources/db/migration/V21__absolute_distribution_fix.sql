-- V21: Absolute Distribution and Ownership Synchronization
-- This is the definitive script to ensure the Corporate user and Store are locked together
-- and orders are correctly split across the platform.

DO $$ 
DECLARE 
    target_corp_id BIGINT;
    target_elite_id BIGINT;
    target_default_id BIGINT;
BEGIN
    -- 1. FORCE create/update Corporate User
    INSERT INTO users (email, name, password_hash, role_type, provider, is_verified, two_factor_enabled, failed_login_attempts, created_at)
    VALUES ('corporate@example.com', 'Elite Tech Manager', '$2b$10$RdVDouPi1pjs0QdjxTHo7OGy/6Y.liMGy/Zw5I7TpmakYP9BvuIYq', 'CORPORATE', 'LOCAL', true, false, 0, NOW())
    ON CONFLICT (email) DO UPDATE SET role_type = 'CORPORATE', name = 'Elite Tech Manager';

    SELECT id INTO target_corp_id FROM users WHERE email = 'corporate@example.com';

    -- 2. FORCE create/update Elite Tech Solutions Store
    -- We delete any duplicate store names to ensure a clean ID link
    DELETE FROM stores WHERE name = 'Elite Tech Solutions' AND owner_id != target_corp_id;
    
    INSERT INTO stores (owner_id, name, description, status, created_at)
    VALUES (target_corp_id, 'Elite Tech Solutions', 'Premium electronics and gadgets.', 'OPEN', NOW())
    ON CONFLICT DO NOTHING;

    -- Retrieve IDs
    SELECT id INTO target_elite_id FROM stores WHERE name = 'Elite Tech Solutions' LIMIT 1;
    SELECT id INTO target_default_id FROM stores WHERE name = 'Default E-Commerce Store' LIMIT 1;

    -- Final ownership enforce
    UPDATE stores SET owner_id = target_corp_id, status = 'OPEN' WHERE id = target_elite_id;

    -- 3. Surgical Product Re-distribution
    -- We ensure EVERY product belongs to a valid store
    UPDATE products SET store_id = target_default_id WHERE store_id IS NULL;
    
    -- Move the 11 "Active" products (the ones with orders) to their assigned stores
    -- Default Store Products (6)
    UPDATE products SET store_id = target_default_id WHERE id IN (19, 21, 219, 23, 25, 9099);
    -- Elite Store Products (5)
    UPDATE products SET store_id = target_elite_id WHERE id IN (18, 20, 22, 24, 58);

    -- 4. ATOMIC Order Synchronization
    -- We force every order to match the store that owns its product.
    -- This is the fastest method for 50k rows on a low-RAM VPS.
    UPDATE orders o
    SET store_id = p.store_id
    FROM order_items oi
    JOIN products p ON oi.product_id = p.id
    WHERE o.id = oi.order_id
    AND p.id IN (18, 19, 20, 21, 219, 22, 23, 24, 25, 58, 9099);

    -- 5. Data Integrity Cleanup
    UPDATE orders SET status = 'DELIVERED' WHERE status IN ('RECEIVED', 'PENDING');
    UPDATE orders SET grand_total = 1.00 WHERE grand_total <= 0 OR grand_total IS NULL;

END $$;
