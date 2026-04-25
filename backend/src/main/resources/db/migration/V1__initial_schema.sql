-- V1: Initial schema

CREATE TABLE users (
    id                          BIGSERIAL PRIMARY KEY,
    name                        VARCHAR(255) NOT NULL,
    email                       VARCHAR(255) NOT NULL UNIQUE,
    password_hash               VARCHAR(255),
    provider                    VARCHAR(50)  NOT NULL DEFAULT 'LOCAL',
    google_id                   VARCHAR(255),
    role_type                   VARCHAR(50)  NOT NULL,
    is_verified                 BOOLEAN      NOT NULL DEFAULT FALSE,
    verification_code           VARCHAR(255),
    verification_code_expires_at TIMESTAMP,
    password_reset_code         VARCHAR(255),
    password_reset_expires_at   TIMESTAMP,
    failed_login_attempts       INT          NOT NULL DEFAULT 0,
    locked_until                TIMESTAMP,
    two_factor_enabled          BOOLEAN      NOT NULL DEFAULT FALSE,
    totp_secret                 VARCHAR(255),
    created_at                  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(512) NOT NULL UNIQUE,
    device_name VARCHAR(255),
    expires_at  TIMESTAMP    NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    user_email  VARCHAR(255),
    event_type  VARCHAR(100) NOT NULL,
    description TEXT,
    ip_address  VARCHAR(50),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE categories (
    id        BIGSERIAL PRIMARY KEY,
    name      VARCHAR(255) NOT NULL,
    parent_id BIGINT REFERENCES categories(id) ON DELETE SET NULL
);

CREATE TABLE stores (
    id         BIGSERIAL PRIMARY KEY,
    owner_id   BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name       VARCHAR(255) NOT NULL,
    description TEXT,
    status     VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE products (
    id                 BIGSERIAL PRIMARY KEY,
    store_id           BIGINT         NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    category_id        BIGINT         REFERENCES categories(id) ON DELETE SET NULL,
    sku                VARCHAR(255)   NOT NULL UNIQUE,
    name               VARCHAR(255)   NOT NULL,
    description        TEXT,
    unit_price         NUMERIC(12, 2) NOT NULL,
    stock_quantity     INT            NOT NULL DEFAULT 0,
    product_importance VARCHAR(50),
    active             BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE TABLE orders (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT         NOT NULL REFERENCES users(id),
    store_id          BIGINT         NOT NULL REFERENCES stores(id),
    status            VARCHAR(50)    NOT NULL DEFAULT 'PENDING',
    grand_total       NUMERIC(12, 2) NOT NULL,
    payment_method    VARCHAR(100),
    shipping_address  TEXT,
    fulfilment        VARCHAR(100),
    sales_channel     VARCHAR(100),
    ship_service_level VARCHAR(100),
    increment_id      VARCHAR(255),
    created_at        TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE TABLE order_items (
    id         BIGSERIAL PRIMARY KEY,
    order_id   BIGINT         NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT         NOT NULL REFERENCES products(id),
    quantity   INT            NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL
);

CREATE TABLE shipments (
    id                   BIGSERIAL PRIMARY KEY,
    order_id             BIGINT         NOT NULL UNIQUE REFERENCES orders(id) ON DELETE CASCADE,
    tracking_number      VARCHAR(255)   UNIQUE,
    carrier              VARCHAR(100),
    warehouse_block      VARCHAR(50),
    mode_of_shipment     VARCHAR(100),
    status               VARCHAR(50)    NOT NULL DEFAULT 'PENDING',
    estimated_delivery   TIMESTAMP,
    delivered_at         TIMESTAMP,
    latitude             DOUBLE PRECISION,
    longitude            DOUBLE PRECISION,
    last_location_update TIMESTAMP,
    customer_care_calls  INT,
    customer_rating      INT,
    cost_of_product      NUMERIC(12, 2),
    prior_purchases      INT,
    discount_offered     NUMERIC(5, 2),
    created_at           TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE TABLE reviews (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id          BIGINT    NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    star_rating         INT       NOT NULL CHECK (star_rating BETWEEN 1 AND 5),
    comment             TEXT,
    helpful_votes       INT       NOT NULL DEFAULT 0,
    total_votes         INT       NOT NULL DEFAULT 0,
    sentiment           VARCHAR(50),
    store_response      TEXT,
    store_responded_at  TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, product_id)
);

CREATE TABLE customer_profiles (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT         NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    gender           VARCHAR(50),
    age              INT,
    city             VARCHAR(255),
    membership_type  VARCHAR(50)    DEFAULT 'BRONZE',
    total_spend      NUMERIC(12, 2) DEFAULT 0,
    items_purchased  INT            DEFAULT 0,
    avg_rating       NUMERIC(3, 2)  DEFAULT 0,
    discount_applied BOOLEAN        DEFAULT FALSE,
    satisfaction_level VARCHAR(50)
);

-- Indexes for frequently queried fields
CREATE INDEX idx_orders_user_id       ON orders(user_id);
CREATE INDEX idx_orders_store_id      ON orders(store_id);
CREATE INDEX idx_orders_status        ON orders(status);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_products_store_id    ON products(store_id);
CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_reviews_product_id   ON reviews(product_id);
CREATE INDEX idx_shipments_order_id   ON shipments(order_id);
CREATE INDEX idx_audit_logs_email     ON audit_logs(user_email);
