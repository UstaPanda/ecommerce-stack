-- ═══════════════════════════════════════════════════════════════
-- V4: Wishlist, Collections, Saved Payment Methods
-- ═══════════════════════════════════════════════════════════════

-- ─── Wishlist ────────────────────────────────────────────────
CREATE TABLE wishlist_items (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id  BIGINT       NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    added_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_wishlist_user_product UNIQUE (user_id, product_id)
);

CREATE INDEX idx_wishlist_user ON wishlist_items(user_id);

-- ─── Collections ─────────────────────────────────────────────
CREATE TABLE collections (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_collections_user ON collections(user_id);

CREATE TABLE collection_items (
    id            BIGSERIAL PRIMARY KEY,
    collection_id BIGINT    NOT NULL REFERENCES collections(id) ON DELETE CASCADE,
    product_id    BIGINT    NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    added_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_collection_product UNIQUE (collection_id, product_id)
);

CREATE INDEX idx_collection_items_collection ON collection_items(collection_id);

-- ─── Saved Payment Methods ────────────────────────────────────
CREATE TABLE saved_payment_methods (
    id                        BIGSERIAL    PRIMARY KEY,
    user_id                   BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type                      VARCHAR(20)  NOT NULL,   -- STRIPE | PAYPAL | CRYPTO
    label                     VARCHAR(100) NOT NULL,
    -- Stripe
    card_last4                VARCHAR(4),
    card_brand                VARCHAR(30),
    stripe_payment_method_id  VARCHAR(100),
    -- PayPal
    paypal_email              VARCHAR(255),
    -- Crypto
    wallet_address            VARCHAR(100),
    chain_id                  INT,
    chain_name                VARCHAR(50),
    is_default                BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at                TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_methods_user ON saved_payment_methods(user_id);
