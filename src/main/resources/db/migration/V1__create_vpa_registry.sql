CREATE TABLE vpa_registry (
    vpa            VARCHAR(100)  PRIMARY KEY,
    bank_code      VARCHAR(20)   NOT NULL,
    bank_api_url   VARCHAR(255)  NOT NULL,
    account_number VARCHAR(50)   NOT NULL,
    created_at     TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_vpa_registry_bank_code ON vpa_registry(bank_code);
