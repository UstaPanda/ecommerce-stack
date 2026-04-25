-- V20: Master Performance and Distribution Overhaul
-- This migration combines all fixing logic (V17, V18, V19) into one 
-- highly optimized script designed for fast execution on low-memory VPS.

-- 1. Indexing for speed (This makes the 50,000 order update take seconds)
CREATE INDEX IF NOT EXISTS idx_oi_order_prod_fast ON order_items (order_id, product_id);
CREATE INDEX IF NOT EXISTS idx_p_store_fast ON products (id, store_id);

DO $$ 
DECLARE 
    elite_id BIGINT;
    corp_id BIGINT;
    default_id BIGINT;
BEGIN
    -- 2. Map the Key IDs
    SELECT id INTO elite_id FROM stores WHERE name = 'Elite Tech Solutions' LIMIT 1;
    SELECT id INTO corp_id FROM users WHERE email = 'corporate@example.com' LIMIT 1;
    SELECT id INTO default_id FROM stores WHERE name = 'Default E-Commerce Store' LIMIT 1;

    -- 3. Fast Ownership and Role Fix
    IF corp_id IS NOT NULL THEN
        UPDATE users SET role_type = 'CORPORATE' WHERE id = corp_id;
        IF elite_id IS NOT NULL THEN
            UPDATE stores SET owner_id = corp_id, status = 'OPEN' WHERE id = elite_id;
        END IF;
    END IF;

    -- 4. Fast Global Data Reset (Optimization: only update if needed)
    IF default_id IS NOT NULL THEN
        UPDATE products SET store_id = default_id WHERE store_id IS NULL;
    END IF;

    -- 5. SURGICAL Product Split
    -- We split the "revenue generator" products (the only ones used in orders) 50/50.
    UPDATE products SET store_id = default_id WHERE id IN (19, 21, 219, 23, 25, 9099);
    IF elite_id IS NOT NULL THEN
        UPDATE products SET store_id = elite_id WHERE id IN (18, 20, 22, 24, 58);
    END IF;

    -- 6. HIGH-PERFORMANCE Order Re-assignment
    -- This uses the indexes created above to update 50,000 orders in one pass.
    UPDATE orders o
    SET store_id = sub.new_store_id
    FROM (
        SELECT oi.order_id, p.store_id as new_store_id
        FROM order_items oi
        JOIN products p ON oi.product_id = p.id
        WHERE p.id IN (18, 19, 20, 21, 219, 22, 23, 24, 25, 58, 9099)
    ) sub
    WHERE o.id = sub.order_id;

    -- 7. Recalculate Totals (Mathematically Optimized)
    -- We only update orders where items actually exist.
    UPDATE orders o
    SET grand_total = sub.total
    FROM (
        SELECT order_id, SUM(quantity * unit_price) as total
        FROM order_items
        GROUP BY order_id
    ) sub
    WHERE o.id = sub.order_id;

END $$;

-- 8. Drop temporary indexes to save disk space
DROP INDEX IF EXISTS idx_oi_order_prod_fast;
DROP INDEX IF EXISTS idx_p_store_fast;

-- 9. Final Safety Cleanup
UPDATE orders SET grand_total = 0.01 WHERE grand_total IS NULL OR grand_total <= 0;
UPDATE orders SET status = 'DELIVERED' WHERE status = 'RECEIVED';
