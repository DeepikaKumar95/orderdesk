CREATE TABLE idempotency_keys (
    idem_key    NVARCHAR(100)    NOT NULL PRIMARY KEY,
    order_id    UNIQUEIDENTIFIER NOT NULL,
    created_at  DATETIME2(3)     NOT NULL DEFAULT SYSUTCDATETIME()
);
