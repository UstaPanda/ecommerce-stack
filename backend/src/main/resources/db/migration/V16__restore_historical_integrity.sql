-- V16: Lightweight Metadata Update
-- Heavy logic moved to V24 for optimized execution.
UPDATE users SET is_verified = true WHERE role_type = 'ADMIN';
