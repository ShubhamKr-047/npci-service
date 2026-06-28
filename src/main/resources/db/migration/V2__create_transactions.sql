CREATE TABLE transactions (
    transaction_id UUID          PRIMARY KEY,
    payer_vpa      VARCHAR(100)  NOT NULL,
    payee_vpa      VARCHAR(100)  NOT NULL,
    amount_paise   BIGINT        NOT NULL,
    status         VARCHAR(50)   NOT NULL,
    payer_rrn      VARCHAR(50),
    payee_rrn      VARCHAR(50),
    failure_reason VARCHAR(255),
    version        BIGINT        NOT NULL DEFAULT 0,
    created_at     TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP     NOT NULL DEFAULT NOW(),

    CONSTRAINT amount_positive CHECK (amount_paise > 0)
);

CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_payer_vpa ON transactions(payer_vpa);
CREATE INDEX idx_transactions_payee_vpa ON transactions(payee_vpa);
