CREATE TABLE IF NOT EXISTS orders
(
    id          UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID           NOT NULL,
    product_id  UUID           NOT NULL,
    quantity    INTEGER        NOT NULL CHECK (quantity > 0),
    total_price NUMERIC(10, 2) NOT NULL CHECK (total_price > 0),
    status      VARCHAR(50)    NOT NULL DEFAULT 'PENDING'
                               CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED')),
    created_at  TIMESTAMPTZ    NOT NULL,
    updated_at  TIMESTAMPTZ    NOT NULL
);

CREATE INDEX idx_orders_customer_id ON orders (customer_id);
CREATE INDEX idx_orders_status ON orders (status);