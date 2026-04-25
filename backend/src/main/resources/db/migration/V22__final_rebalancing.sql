-- V22: Lightweight Metadata Update
-- Heavy logic moved to V24 for optimized execution.
UPDATE users SET role_type = 'CORPORATE' WHERE email = 'corporate@example.com';
