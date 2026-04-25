-- V21: Direct Store Ownership and Order Distribution Fix
-- This script definitively links the corporate user to their store and splits the orders.

DO $$ 
DECLARE 
    target_user_id BIGINT;
    target_store_id BIGINT;
BEGIN
    -- 1. Ensure the Corporate User exists and has the correct role
    INSERT INTO users (email, name, password_hash, role_type, provider, is_verified, two_factor_enabled, failed_login_attempts, created_at)
    VALUES ('corporate@example.com', 'Elite Tech Manager', '$2b$10$RdVDouPi1pjs0QdjxTHo7OGy/6Y.liMGy/Zw5I7TpmakYP9BvuIYq', 'CORPORATE', 'LOCAL', true, false, 0, NOW())
    ON CONFLICT (email) DO UPDATE SET role_type = 'CORPORATE';

    SELECT id INTO target_user_id FROM users WHERE email = 'corporate@example.com';

    -- 2. Ensure the Elite Tech Solutions store exists and is owned by this user
    INSERT INTO stores (owner_id, name, description, status, created_at)
    VALUES (target_user_id, 'Elite Tech Solutions', 'Premium gadgets and electronics.', 'OPEN', NOW())
    ON CONFLICT (name) DO UPDATE SET owner_id = target_user_id, status = 'OPEN';

    SELECT id INTO target_store_id FROM stores WHERE name = 'Elite Tech Solutions';

    -- 3. Move the 'Active' products (those that have historical orders) to the store
    -- We split the 11 active products 50/50.
    -- Moving these 5 products to Elite Tech Solutions
    UPDATE products SET store_id = target_store_id WHERE id IN (18, 20, 22, 24, 58);

    -- 4. Move the Orders to the correct store
    -- Every order that contains an Elite Tech product must belong to the Elite Tech store.
    UPDATE orders o
    SET store_id = target_store_id
    WHERE EXISTS (
        SELECT 1 FROM order_items oi
        WHERE oi.order_id = o.id 
        AND oi.product_id IN (18, 20, 22, 24, 58)
    );

    -- 5. Mathematical cleanup: ensure totals match the split
    UPDATE orders o
    SET grand_total = (SELECT SUM(quantity * unit_price) FROM order_items WHERE order_id = o.id)
    WHERE store_id = target_store_id;

END $$;
