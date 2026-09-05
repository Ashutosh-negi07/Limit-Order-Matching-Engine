CREATE TABLE orders (
    id                 UUID          PRIMARY KEY,
    user_id            BIGINT        NOT NULL,
    instrument         VARCHAR(20)   NOT NULL,
    side               VARCHAR(4)    NOT NULL,
    limit_price        NUMERIC(19,4) NOT NULL,
    original_quantity  BIGINT        NOT NULL,
    remaining_quantity BIGINT        NOT NULL,
    status             VARCHAR(20)   NOT NULL,
    sequence_number    BIGINT        NOT NULL,
    created_at         TIMESTAMPTZ   NOT NULL,
    updated_at         TIMESTAMPTZ   NOT NULL
);
