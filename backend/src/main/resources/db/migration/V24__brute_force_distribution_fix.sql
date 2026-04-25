-- V24: Ultimate Brute Force Optimized Distribution Fix
-- Combined, optimized logic to ensure 50k orders are split in seconds on 2GB RAM.

DO $$ 
DECLARE 
    final_corp_id BIGINT;
    final_elite_id BIGINT;
    final_default_id BIGINT;
BEGIN
    -- 1. CLEAN BRANDING & SETTINGS
    UPDATE platform_settings SET platform_name = 'ZorluKurt Trading', support_email = 'support@zorlukurt.com' WHERE id = 1;

    -- 2. REPAIR USER: Ensure corporate@example.com is the verified manager
    INSERT INTO users (email, name, password_hash, role_type, provider, is_verified, two_factor_enabled, failed_login_attempts, created_at)
    VALUES ('corporate@example.com', 'Elite Tech Manager', '$2b$10$RdVDouPi1pjs0QdjxTHo7OGy/6Y.liMGy/Zw5I7TpmakYP9BvuIYq', 'CORPORATE', 'LOCAL', true, false, 0, NOW())
    ON CONFLICT (email) DO UPDATE SET role_type = 'CORPORATE', is_verified = true;

    SELECT id INTO final_corp_id FROM users WHERE email = 'corporate@example.com';

    -- 3. REPAIR STORES: Ensure both exist and link them by ID
    SELECT id INTO final_default_id FROM stores WHERE name = 'Default E-Commerce Store' LIMIT 1;
    SELECT id INTO final_elite_id FROM stores WHERE name = 'Elite Tech Solutions' LIMIT 1;

    IF final_elite_id IS NULL THEN
        INSERT INTO stores (owner_id, name, description, status, created_at)
        VALUES (final_corp_id, 'Elite Tech Solutions', 'Premium gadgets and electronics.', 'OPEN', NOW())
        RETURNING id INTO final_elite_id;
    ELSE
        UPDATE stores SET owner_id = final_corp_id, status = 'OPEN' WHERE id = final_elite_id;
    END IF;

    -- 4. BATCH OPTIMIZED DATA SPLIT (50,000 rows in seconds)
    -- We use a single mathematical update based on the primary key for max performance.
    UPDATE orders SET store_id = CASE WHEN id % 2 = 0 THEN final_elite_id ELSE final_default_id END;
    UPDATE products SET store_id = CASE WHEN id % 2 = 0 THEN final_elite_id ELSE final_default_id END;

    -- 5. HIGH-SPEED TOTAL RECALCULATION
    -- Instead of correlated subqueries, we use a temporary aggregation table.
    CREATE TEMP TABLE tmp_order_totals AS 
    SELECT order_id, SUM(quantity * unit_price) as calculated_total
    FROM order_items
    GROUP BY order_id;

    CREATE INDEX idx_tmp_totals ON tmp_order_totals(order_id);

    UPDATE orders o
    SET grand_total = t.calculated_total
    FROM tmp_order_totals t
    WHERE o.id = t.order_id;

    DROP TABLE tmp_order_totals;

    -- 6. FINAL DATA CLEANUP
    UPDATE orders SET status = 'DELIVERED' WHERE status IN ('RECEIVED', 'PENDING');
    UPDATE orders SET grand_total = 19.99 WHERE grand_total <= 0 OR grand_total IS NULL;

END $$;
