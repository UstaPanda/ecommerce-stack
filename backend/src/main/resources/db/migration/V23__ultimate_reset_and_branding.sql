-- V23: Ultimate Atomic Data Reset and Branding Overhaul
-- This is the FINAL fix for store distribution, corporate ownership, and branding consistency.

DO $$ 
DECLARE 
    target_user_id BIGINT;
    target_store_id BIGINT;
    default_store_id BIGINT;
BEGIN
    -- 1. BRANDING: Force platform name in settings table
    UPDATE platform_settings SET platform_name = 'ZorluKurt Trading', support_email = 'support@zorlukurt.com' WHERE id = 1;

    -- 2. USER: Ensure corporate@example.com is the ONLY corporate user and is verified
    -- We delete any potential duplicates to ensure a clean state
    DELETE FROM users WHERE email = 'corporate@example.com';
    
    INSERT INTO users (email, name, password_hash, role_type, provider, is_verified, two_factor_enabled, failed_login_attempts, created_at)
    VALUES ('corporate@example.com', 'Elite Tech Manager', '$2b$10$RdVDouPi1pjs0QdjxTHo7OGy/6Y.liMGy/Zw5I7TpmakYP9BvuIYq', 'CORPORATE', 'LOCAL', true, false, 0, NOW())
    RETURNING id INTO target_user_id;

    -- 3. STORE: Ensure Elite Tech Solutions is OWNED by this user
    -- Reset the Elite store if it exists, or create it fresh
    SELECT id INTO target_store_id FROM stores WHERE name = 'Elite Tech Solutions' LIMIT 1;
    
    IF target_store_id IS NOT NULL THEN
        UPDATE stores SET owner_id = target_user_id, status = 'OPEN' WHERE id = target_store_id;
    ELSE
        INSERT INTO stores (owner_id, name, description, status, created_at)
        VALUES (target_user_id, 'Elite Tech Solutions', 'Premium gadgets and electronics.', 'OPEN', NOW())
        RETURNING id INTO target_store_id;
    END IF;

    -- Ensure Default Store exists
    SELECT id INTO default_store_id FROM stores WHERE name = 'Default E-Commerce Store' LIMIT 1;
    IF default_store_id IS NULL THEN
        -- Fallback if store was deleted
        INSERT INTO stores (owner_id, name, description, status, created_at)
        VALUES ((SELECT id FROM users WHERE role_type = 'ADMIN' LIMIT 1), 'Default E-Commerce Store', 'Main platform store.', 'OPEN', NOW())
        RETURNING id INTO default_store_id;
    END IF;

    -- 4. DATA SPLIT: Move exactly half of ALL products and their orders to the Elite store
    -- Step A: Set all to Default first for a clean split
    UPDATE products SET store_id = default_store_id;
    UPDATE orders SET store_id = default_store_id;

    -- Step B: Move half of products to Elite store (Even IDs)
    UPDATE products SET store_id = target_store_id WHERE id % 2 = 0;

    -- Step C: Synchronize Orders based on the products they contain
    -- High-performance atomic update using a temporary mapping
    UPDATE orders o
    SET store_id = p.store_id
    FROM order_items oi
    JOIN products p ON oi.product_id = p.id
    WHERE o.id = oi.order_id;

    -- 5. MATH: Final total verification
    UPDATE orders o
    SET grand_total = sub.calculated_total
    FROM (
        SELECT order_id, SUM(quantity * unit_price) as calculated_total
        FROM order_items
        GROUP BY order_id
    ) sub
    WHERE o.id = sub.order_id;

    -- Safety fallback for orders without items
    UPDATE orders SET grand_total = 1.00 WHERE grand_total <= 0 OR grand_total IS NULL;

END $$;
