-- V13: Add sample orders to the second store (Elite Tech Solutions)

-- 1. Create a sample order for 'admin@example.com' buying from the new store
INSERT INTO orders (
    user_id, 
    store_id, 
    status, 
    grand_total, 
    payment_method, 
    shipping_address, 
    created_at, 
    updated_at
)
SELECT 
    (SELECT id FROM users WHERE email = 'admin@example.com'),
    (SELECT id FROM stores WHERE name = 'Elite Tech Solutions'),
    'DELIVERED',
    13.31,
    'STRIPE',
    '123 Admin Way, Tech City, TC 12345',
    NOW() - INTERVAL '2 days',
    NOW() - INTERVAL '2 days'
WHERE EXISTS (SELECT 1 FROM stores WHERE name = 'Elite Tech Solutions');

-- 2. Add items to the first order (Product 27: 9.96, Product 17: 1.66 * 2 + Product 29: 0.83 = 13.31)
INSERT INTO order_items (order_id, product_id, quantity, unit_price)
SELECT 
    curr.id, 
    p.id, 
    CASE WHEN p.id = 27 THEN 1 WHEN p.id = 17 THEN 2 ELSE 1 END,
    p.unit_price
FROM orders curr
JOIN products p ON p.id IN (27, 17, 29)
WHERE curr.shipping_address = '123 Admin Way, Tech City, TC 12345'
AND NOT EXISTS (SELECT 1 FROM order_items WHERE order_id = curr.id);

-- 3. Create another order for a different customer (user_381@example.com)
INSERT INTO orders (
    user_id, 
    store_id, 
    status, 
    grand_total, 
    payment_method, 
    shipping_address, 
    created_at, 
    updated_at
)
SELECT 
    (SELECT id FROM users WHERE email = 'user_381@example.com'),
    (SELECT id FROM stores WHERE name = 'Elite Tech Solutions'),
    'CONFIRMED',
    11.65,
    'CRYPTO_WALLET',
    '789 Blockchain Ave, Crypto Valley, CV 99',
    NOW() - INTERVAL '1 day',
    NOW() - INTERVAL '1 day'
WHERE EXISTS (SELECT 1 FROM stores WHERE name = 'Elite Tech Solutions');

-- 4. Add items to the second order (Product 28: 1.69, Product 27: 9.96 = 11.65)
INSERT INTO order_items (order_id, product_id, quantity, unit_price)
SELECT 
    curr.id, 
    p.id, 
    1,
    p.unit_price
FROM orders curr
JOIN products p ON p.id IN (28, 27)
WHERE curr.shipping_address = '789 Blockchain Ave, Crypto Valley, CV 99'
AND NOT EXISTS (SELECT 1 FROM order_items WHERE order_id = curr.id);
