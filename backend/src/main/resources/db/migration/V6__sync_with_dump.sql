-- V6: Sync schema with production dump (completeihope-aadtest-202604170213)
-- All changes are additive or rename-only — zero data loss.

-- ─── 1. audit_logs ───────────────────────────────────────────────────────────
-- Rename 'description' → 'details' (existing data is preserved)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'audit_logs' AND column_name = 'description'
    ) THEN
        ALTER TABLE audit_logs RENAME COLUMN description TO details;
    END IF;
END $$;

-- Add 'device' column
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS device VARCHAR(255);

-- ─── 2. refresh_tokens ───────────────────────────────────────────────────────
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS ip_address   VARCHAR(255);
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS last_seen    TIMESTAMP;
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS revoked      BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS remember_me  BOOLEAN NOT NULL DEFAULT FALSE;

-- ─── 3. orders ───────────────────────────────────────────────────────────────
ALTER TABLE orders ADD COLUMN IF NOT EXISTS tx_hash VARCHAR(255);

-- ─── 4. user_addresses (new table) ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS user_addresses (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title        VARCHAR(255) NOT NULL,
    full_address VARCHAR(255) NOT NULL,
    city         VARCHAR(255) NOT NULL,
    district     VARCHAR(255),
    postal_code  VARCHAR(255),
    phone        VARCHAR(255),
    is_default   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_addresses_user_id ON user_addresses(user_id);
