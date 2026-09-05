CREATE TABLE trades (
    id              UUID          PRIMARY KEY,
    buy_order_id    UUID          NOT NULL REFERENCES orders(id),
    sell_order_id   UUID          NOT NULL REFERENCES orders(id),
    instrument      VARCHAR(20)   NOT NULL,
    execution_price NUMERIC(19,4) NOT NULL,
    quantity        BIGINT        NOT NULL,
    executed_at     TIMESTAMPTZ   NOT NULL
);
