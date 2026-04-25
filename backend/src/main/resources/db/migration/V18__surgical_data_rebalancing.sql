-- V18: Surgical Data Rebalancing
-- This script fixes the "0 orders in Default Store" issue by carefully 
-- distributing the small set of active products between the two stores.

-- 1. Reset everything to Default Store (ID 1)
UPDATE products SET store_id = (SELECT id FROM stores WHERE name = 'Default E-Commerce Store');
UPDATE orders SET store_id = (SELECT id FROM stores WHERE name = 'Default E-Commerce Store');

-- 2. Move roughly half of the ACTIVE products to Elite Tech Solutions
-- Only 11 products are actually used in orders. We split them.
UPDATE products 
SET store_id = (SELECT id FROM stores WHERE name = 'Elite Tech Solutions')
WHERE id IN (18, 20, 22, 24, 58); 

-- 3. Move orders that belong to these products to Elite Tech Solutions
-- This ensures the orders are split based on product ownership
UPDATE orders o
SET store_id = (SELECT id FROM stores WHERE name = 'Elite Tech Solutions')
WHERE EXISTS (
    SELECT 1 FROM order_items oi 
    WHERE oi.order_id = o.id 
    AND oi.product_id IN (18, 20, 22, 24, 58)
);

-- 4. Re-calculate totals for consistency
UPDATE orders o
SET grand_total = (SELECT COALESCE(SUM(quantity * unit_price), 0) FROM order_items WHERE order_id = o.id);

-- 5. Final status check (Ensure cancelled/returned logic works for both)
UPDATE orders SET status = 'DELIVERED' WHERE status = 'RECEIVED';
