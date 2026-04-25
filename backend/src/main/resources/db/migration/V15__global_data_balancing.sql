-- V15: Lightweight Metadata Update
-- Heavy logic moved to V24 for optimized execution.
UPDATE stores SET status = 'OPEN' WHERE name = 'Default E-Commerce Store';
