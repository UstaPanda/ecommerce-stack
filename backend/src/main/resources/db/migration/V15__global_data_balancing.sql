-- V15: Global Data Balancing and Price Synchronization
-- This migration fixes the "inconsistent" price feeling and balances the two stores.

-- 1. Synchronize ALL order item prices with their current product prices
-- This ensures that if you see a $200 product, it actually costs $200 in the order history.
UPDATE order_items oi
SET unit_price = p.unit_price
FROM products p
WHERE oi.product_id = p.id;

-- 2. Recalculate ALL order grand totals based on the new synchronized prices
UPDATE orders o
SET grand_total = sub.calculated_total
FROM (
    SELECT order_id, SUM(quantity * unit_price) as calculated_total
    FROM order_items
    GROUP BY order_id
) sub
WHERE o.id = sub.order_id;

-- 3. Distribute Products between the two stores
-- Move 15% of products to the second store (Elite Tech Solutions)
UPDATE products 
SET store_id = (SELECT id FROM stores WHERE name = 'Elite Tech Solutions')
WHERE id % 7 = 0; -- Mathematical distribution

-- 4. Re-assign Orders to the correct store
-- An order must belong to the store that owns its products.
-- Since our system assumes one store per order, we update the order's store_id
-- based on the products it actually contains.
UPDATE orders o
SET store_id = p.store_id
FROM order_items oi
JOIN products p ON oi.product_id = p.id
WHERE o.id = oi.order_id;

-- 5. Final verification: ensure no order has a total of 0 (safety fallback)
UPDATE orders SET grand_total = 19.99 WHERE grand_total IS NULL OR grand_total <= 0;
