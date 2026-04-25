-- V7: Add missing tables and columns that exist in entities but not in prior migrations

-- ─── 1. users: facebook_id column ────────────────────────────────────────────
ALTER TABLE users ADD COLUMN IF NOT EXISTS facebook_id VARCHAR(255);

-- ─── 2. stores: address column ───────────────────────────────────────────────
ALTER TABLE stores ADD COLUMN IF NOT EXISTS address VARCHAR(500);

-- ─── 3. coupons table ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS coupons (
    id             BIGSERIAL    PRIMARY KEY,
    code           VARCHAR(100) NOT NULL UNIQUE,
    discount_type  VARCHAR(20)  NOT NULL,   -- PERCENTAGE | FIXED
    discount_value NUMERIC(10,2) NOT NULL,
    store_id       BIGINT       REFERENCES stores(id) ON DELETE SET NULL,
    max_uses       INT,
    used_count     INT          NOT NULL DEFAULT 0,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    expires_at     TIMESTAMP,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_coupons_code     ON coupons(code);
CREATE INDEX IF NOT EXISTS idx_coupons_store_id ON coupons(store_id);

-- ─── 4. platform_settings table ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS platform_settings (
    id                        BIGINT  PRIMARY KEY DEFAULT 1,
    maintenance_mode          BOOLEAN NOT NULL DEFAULT FALSE,
    allow_registrations       BOOLEAN NOT NULL DEFAULT TRUE,
    require_email_verification BOOLEAN NOT NULL DEFAULT TRUE,
    max_orders_per_user       INT     NOT NULL DEFAULT 50,
    platform_currency         VARCHAR(10)  NOT NULL DEFAULT 'USD',
    support_email             VARCHAR(255) NOT NULL DEFAULT 'support@zorlukurt.com',
    platform_name             VARCHAR(255) NOT NULL DEFAULT 'ZorluKurt Trading',
    updated_at                TIMESTAMP
);

-- Seed default row so GET /api/admin/settings returns data immediately
INSERT INTO platform_settings (id, maintenance_mode, allow_registrations,
    require_email_verification, max_orders_per_user,
    platform_currency, support_email, platform_name)
VALUES (1, FALSE, TRUE, TRUE, 50, 'USD', 'support@zorlukurt.com', 'ZorluKurt Trading')
ON CONFLICT (id) DO NOTHING;
