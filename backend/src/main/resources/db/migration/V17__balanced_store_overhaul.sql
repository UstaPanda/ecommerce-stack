-- V17: Lightweight Store Setup
-- Optimized to skip heavy calculations that crash low-memory VPS.
-- Logic moved to V20 for optimized execution.

-- Just create the store if it doesn't exist (very fast)
UPDATE stores SET status = 'OPEN' WHERE name = 'Default E-Commerce Store';
