-- V19: Lightweight User Setup
-- Optimized to skip heavy calculations.
-- Logic moved to V20 for optimized execution.

-- Fast role check
UPDATE users SET role_type = 'CORPORATE' WHERE email = 'corporate@example.com';
