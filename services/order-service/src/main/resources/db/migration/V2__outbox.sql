CREATE TABLE outbox_events (
    id             UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    aggregate_type NVARCHAR(40)     NOT NULL,
    aggregate_id   NVARCHAR(64)     NOT NULL,
    event_type     NVARCHAR(60)     NOT NULL,
    payload        NVARCHAR(MAX)    NOT NULL,
    trace_parent   NVARCHAR(64)     NULL,
    created_at     DATETIME2(3)     NOT NULL DEFAULT SYSUTCDATETIME(),
    published_at   DATETIME2(3)     NULL
);
-- filtered index: the relay only ever scans unpublished rows
CREATE NONCLUSTERED INDEX ix_outbox_unpublished
    ON outbox_events(created_at) WHERE published_at IS NULL;
