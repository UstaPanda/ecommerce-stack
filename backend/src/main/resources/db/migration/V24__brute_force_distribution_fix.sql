-- V24: Brute Force Distribution and Ownership Fix
-- This is a high-performance, non-relational script designed to fix the 
-- store split and ownership regardless of data dump inconsistencies.

DO $$ 
DECLARE 
    final_corp_id BIGINT;
    final_elite_id BIGINT;
    final_default_id BIGINT;
BEGIN
    -- 1. REPAIR USER: Ensure corporate@example.com exists with the correct role
    INSERT INTO users (email, name, password_hash, role_type, provider, is_verified, two_factor_enabled, failed_login_attempts, created_at)
    VALUES ('corporate@example.com', 'Elite Tech Manager', '$2b$10$RdVDouPi1pjs0QdjxTHo7OGy/6Y.liMGy/Zw5I7TpmakYP9BvuIYq', 'CORPORATE', 'LOCAL', true, false, 0, NOW())
    ON CONFLICT (email) DO UPDATE SET 
        role_type = 'CORPORATE', 
        is_verified = true;

    SELECT id INTO final_corp_id FROM users WHERE email = 'corporate@example.com';

    -- 2. REPAIR STORES: Ensure both stores exist and are correctly owned
    SELECT id INTO final_default_id FROM stores WHERE name = 'Default E-Commerce Store' LIMIT 1;
    SELECT id INTO final_elite_id FROM stores WHERE name = 'Elite Tech Solutions' LIMIT 1;

    -- If elite store doesn't exist, create it. If it does, force the owner.
    IF final_elite_id IS NULL THEN
        INSERT INTO stores (owner_id, name, description, status, created_at)
        VALUES (final_corp_id, 'Elite Tech Solutions', 'Premium gadgets and electronics.', 'OPEN', NOW())
        RETURNING id INTO final_elite_id;
    ELSE
        UPDATE stores SET owner_id = final_corp_id, status = 'OPEN' WHERE id = final_elite_id;
    END IF;

    -- 3. BRUTE FORCE SPLIT: Ignore product-order relationships
    -- We split the orders table 50/50 based on the Order ID itself.
    -- This bypasses the "missing products" in the data dump.
    UPDATE orders SET store_id = final_default_id WHERE id % 2 != 0;
    UPDATE orders SET store_id = final_elite_id WHERE id % 2 = 0;

    -- 4. SYNC PRODUCTS: For consistency, split products the same way
    UPDATE products SET store_id = final_default_id WHERE id % 2 != 0;
    UPDATE products SET store_id = final_elite_id WHERE id % 2 = 0;

    -- 5. FINAL CLEANUP
    UPDATE orders SET grand_total = 1.00 WHERE grand_total IS NULL OR grand_total <= 0;
    UPDATE orders SET status = 'DELIVERED' WHERE status IN ('RECEIVED', 'PENDING');

END $$;
