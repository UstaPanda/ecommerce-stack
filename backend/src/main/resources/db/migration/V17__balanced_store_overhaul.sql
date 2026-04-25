-- V17: Balanced Store Overhaul
-- This migration ensures a fair distribution of revenue between the two stores
-- and locks the data for consistent reporting in AI and Dashboard.

-- 1. Reset all orders to the Default Store first for a clean slate
UPDATE orders SET store_id = (SELECT id FROM stores WHERE name = 'Default E-Commerce Store');
UPDATE products SET store_id = (SELECT id FROM stores WHERE name = 'Default E-Commerce Store');

-- 2. Move roughly 30% of Products to Elite Tech Solutions
-- We use a hash of the SKU to ensure it's random but stable
UPDATE products 
SET store_id = (SELECT id FROM stores WHERE name = 'Elite Tech Solutions')
WHERE ABS(hashtext(sku)) % 10 < 3;

-- 3. Move ALL Orders that contain these products to Elite Tech Solutions
-- This ensures that when the AI joins through products, it finds the same store.
UPDATE orders o
SET store_id = (SELECT id FROM stores WHERE name = 'Elite Tech Solutions')
WHERE EXISTS (
    SELECT 1 FROM order_items oi 
    JOIN products p ON oi.product_id = p.id
    WHERE oi.order_id = o.id 
    AND p.store_id = (SELECT id FROM stores WHERE name = 'Elite Tech Solutions')
);

-- 4. Final mathematical check
-- Recalculate totals one last time to ensure they areNet of Items
UPDATE orders o
SET grand_total = (SELECT COALESCE(SUM(quantity * unit_price), 0) FROM order_items WHERE order_id = o.id);

-- 5. Status distribution check
-- Ensure some orders are in different states to show off the status logic
UPDATE orders SET status = 'DELIVERED' WHERE status = 'RECEIVED';
UPDATE orders SET status = 'CANCELLED' WHERE grand_total > 5000 AND id % 10 = 0;
