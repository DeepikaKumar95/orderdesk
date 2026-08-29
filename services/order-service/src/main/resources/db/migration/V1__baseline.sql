CREATE TABLE customers (
    customer_id   NVARCHAR(40)   NOT NULL PRIMARY KEY,
    name          NVARCHAR(200)  NOT NULL,
    credit_limit  DECIMAL(18,2)  NOT NULL DEFAULT 0
);

CREATE TABLE orders (
    order_id     UNIQUEIDENTIFIER NOT NULL PRIMARY KEY DEFAULT NEWSEQUENTIALID(),
    customer_id  NVARCHAR(40)     NOT NULL REFERENCES customers(customer_id),
    status       NVARCHAR(20)     NOT NULL,
    total        DECIMAL(18,2)    NOT NULL,
    version      BIGINT           NOT NULL DEFAULT 0,
    created_at   DATETIME2(3)     NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at   DATETIME2(3)     NOT NULL DEFAULT SYSUTCDATETIME()
);
CREATE NONCLUSTERED INDEX ix_orders_customer_created
    ON orders(customer_id, created_at DESC) INCLUDE (status, total);

CREATE TABLE order_lines (
    order_id    UNIQUEIDENTIFIER NOT NULL REFERENCES orders(order_id),
    line_no     INT              NOT NULL,
    sku         NVARCHAR(40)     NOT NULL,
    qty         INT              NOT NULL CHECK (qty > 0),
    unit_price  DECIMAL(18,2)    NOT NULL,
    CONSTRAINT pk_order_lines PRIMARY KEY CLUSTERED (order_id, line_no)
);

INSERT INTO customers (customer_id, name, credit_limit) VALUES
  ('C-1001', 'Acme Logistics', 50000),
  ('C-1002', 'Peachtree Retail', 10000),
  ('C-1003', 'Cumming Hardware', 2500);
