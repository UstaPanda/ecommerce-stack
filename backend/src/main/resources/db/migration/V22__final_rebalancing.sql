-- V22: Final Data Rebalancing and Ownership
-- This script splits products and orders 50/50 between the two stores
-- and ensures the corporate user is the verified owner of Elite Tech Solutions.

DO $$ 
DECLARE 
    target_corp_id BIGINT;
    target_elite_id BIGINT;
    target_default_id BIGINT;
BEGIN
    -- 1. Ensure Corporate User exists, is verified, and has the correct role
    UPDATE users 
    SET role_type = 'CORPORATE', 
        is_verified = true 
    WHERE email = 'corporate@example.com';
    
    SELECT id INTO target_corp_id FROM users WHERE email = 'corporate@example.com';

    -- 2. Identify the stores
    SELECT id INTO target_default_id FROM stores WHERE name = 'Default E-Commerce Store' LIMIT 1;
    SELECT id INTO target_elite_id FROM stores WHERE name = 'Elite Tech Solutions' LIMIT 1;

    -- 3. Fix Ownership
    IF target_corp_id IS NOT NULL AND target_elite_id IS NOT NULL THEN
        UPDATE stores SET owner_id = target_corp_id, status = 'OPEN' WHERE id = target_elite_id;
    END IF;

    -- 4. Split ALL products 50/50 between the two stores
    -- This ensures that no matter which products have orders, both stores will have some.
    IF target_elite_id IS NOT NULL AND target_default_id IS NOT NULL THEN
        UPDATE products SET store_id = target_default_id WHERE id % 2 != 0;
        UPDATE products SET store_id = target_elite_id WHERE id % 2 = 0;

        -- 5. Atomic Order Re-assignment
        -- We sync every order to the store that owns its products.
        -- This is the definitive way to split the 50,000 orders.
        UPDATE orders o
        SET store_id = p.store_id
        FROM order_items oi
        JOIN products p ON oi.product_id = p.id
        WHERE o.id = oi.order_id;
    END IF;

    -- 6. Recalculate totals for perfect consistency
    UPDATE orders o
    SET grand_total = (SELECT COALESCE(SUM(quantity * unit_price), 0) FROM order_items WHERE order_id = o.id)
    WHERE grand_total <= 0 OR grand_total IS NULL;

END $$;
