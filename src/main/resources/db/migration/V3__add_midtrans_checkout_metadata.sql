ALTER TABLE payment
    ADD COLUMN provider_transaction_id VARCHAR(255) UNIQUE,
    ADD COLUMN checkout_url VARCHAR(255);

ALTER TABLE payment
    DROP CONSTRAINT ck_payment_status;

ALTER TABLE payment
    ADD CONSTRAINT ck_payment_status
        CHECK (
            status IN (
                'PENDING', 'SUCCEEDED', 'FAILED', 'REFUNDED', 'CANCELLED', 'EXPIRED', 'REVIEW_REQUIRED'
            )
        );
