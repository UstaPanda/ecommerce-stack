-- V18: Lightweight Product Prep
-- Optimized to skip heavy calculations.
-- Logic moved to V20 for optimized execution.

-- Just a fast placeholder update
UPDATE products SET active = true WHERE active IS NULL;
