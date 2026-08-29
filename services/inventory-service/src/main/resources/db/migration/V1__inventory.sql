CREATE TABLE inventory (
    sku       NVARCHAR(40) NOT NULL PRIMARY KEY,
    on_hand   INT          NOT NULL CHECK (on_hand >= 0),
    reserved  INT          NOT NULL DEFAULT 0 CHECK (reserved >= 0)
);

CREATE TABLE inventory_reservations (
    reservation_id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY DEFAULT NEWID(),
    order_id       UNIQUEIDENTIFIER NOT NULL,
    sku            NVARCHAR(40)     NOT NULL REFERENCES inventory(sku),
    qty            INT              NOT NULL,
    state          NVARCHAR(20)     NOT NULL,
    created_at     DATETIME2(3)     NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_reservation_order_sku UNIQUE (order_id, sku)
);

-- consumer idempotency: (event_id, consumer_group) is the natural key
CREATE TABLE processed_events (
    event_id       UNIQUEIDENTIFIER NOT NULL,
    consumer_group NVARCHAR(60)     NOT NULL,
    processed_at   DATETIME2(3)     NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT pk_processed_events PRIMARY KEY (event_id, consumer_group)
);

INSERT INTO inventory (sku, on_hand) VALUES ('SKU-1', 100), ('SKU-2', 25), ('SKU-3', 1), ('SKU-9', 500);
