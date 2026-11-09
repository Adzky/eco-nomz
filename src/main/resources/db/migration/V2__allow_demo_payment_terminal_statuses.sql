ALTER TABLE payment
    DROP CONSTRAINT ck_payment_status;

ALTER TABLE payment
    ADD CONSTRAINT ck_payment_status
        CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'REFUNDED', 'CANCELLED', 'EXPIRED'));

CREATE INDEX ix_payment_status_created_at ON payment (status, created_at);
