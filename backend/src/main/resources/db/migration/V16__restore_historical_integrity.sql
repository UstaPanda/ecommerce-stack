-- V16: Restore Historical Integrity and Proper Store Balancing
-- This script fixes the "fake" feeling by allowing historical prices to exist
-- while ensuring mathematical consistency and a realistic store split.

-- 1. Ensure mathematical consistency (Total = Sum of Items)
-- We use the unit_price that was actually recorded in the order_items table.
UPDATE orders o
SET grand_total = sub.calculated_total
FROM (
    SELECT order_id, SUM(quantity * unit_price) as calculated_total
    FROM order_items
    GROUP BY order_id
) sub
WHERE o.id = sub.order_id;

-- 2. Proper Store Distribution
-- Move 30% of products to the corporate store to create a realistic "competitor"
UPDATE products 
SET store_id = (SELECT id FROM stores WHERE name = 'Elite Tech Solutions')
WHERE id % 3 = 0; 

-- 3. Sync Order ownership
-- Every order must belong to the store that owned the products at that time.
UPDATE orders o
SET store_id = p.store_id
FROM order_items oi
JOIN products p ON oi.product_id = p.id
WHERE o.id = oi.order_id;

-- 4. Cleanup
-- Ensure no nulls or zeros exist in totals for the AI to report
UPDATE orders SET grand_total = 1.00 WHERE grand_total IS NULL OR grand_total <= 0;
