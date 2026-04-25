-- V14: Data Cleanup for Analytics Consistency
-- This script fixes the bad sample data in the SQL dump where order totals didn't match item sums.

-- 1. Recalculate grand_total for all existing orders based on their items
-- We do this only for orders where the total is clearly wrong (off by more than 1 unit)
UPDATE orders o
SET grand_total = sub.calculated_total
FROM (
    SELECT order_id, SUM(quantity * unit_price) as calculated_total
    FROM order_items
    GROUP BY order_id
) sub
WHERE o.id = sub.order_id
AND ABS(o.grand_total - sub.calculated_total) > 1.0;

-- 2. Fix users with missing role_type (set to INDIVIDUAL by default)
UPDATE users SET role_type = 'INDIVIDUAL' WHERE role_type IS NULL;

-- 3. Fix users with missing names
UPDATE users SET name = 'Sample Customer' WHERE name IS NULL OR name = '';

-- 4. Ensure all stores have an 'OPEN' status if missing or invalid
UPDATE stores SET status = 'OPEN' WHERE status NOT IN ('OPEN', 'CLOSED', 'PENDING');
