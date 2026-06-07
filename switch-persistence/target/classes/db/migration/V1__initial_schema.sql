-- ============================================================
-- V1 – Initial Payment Switch Schema
-- ============================================================

CREATE TABLE IF NOT EXISTS transactions (
    id               BIGSERIAL PRIMARY KEY,
    message_id       VARCHAR(36)    NOT NULL UNIQUE,
    mti              CHAR(4)        NOT NULL,
    processing_code  CHAR(6),
    pan              VARCHAR(19)    NOT NULL,   -- stored masked
    amount           BIGINT         NOT NULL,   -- minor currency units
    currency_code    CHAR(3),
    stan             CHAR(6),
    rrn              VARCHAR(12),
    acquirer_id      VARCHAR(50),
    issuer_id        VARCHAR(50),
    response_code    CHAR(2),
    status           VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_rrn        ON transactions(rrn);
CREATE INDEX idx_transactions_stan       ON transactions(stan);
CREATE INDEX idx_transactions_status     ON transactions(status);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);

CREATE TABLE IF NOT EXISTS bin_table (
    id               SERIAL PRIMARY KEY,
    bin_range_start  CHAR(6)        NOT NULL,
    bin_range_end    CHAR(6)        NOT NULL,
    issuer_id        VARCHAR(50)    NOT NULL,
    issuer_name      VARCHAR(100)   NOT NULL,
    active           BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_bin_range CHECK (bin_range_start <= bin_range_end)
);

CREATE INDEX idx_bin_table_range  ON bin_table(bin_range_start, bin_range_end);
CREATE INDEX idx_bin_table_active ON bin_table(active);

COMMENT ON TABLE transactions IS 'Audit log of all processed payment messages';
COMMENT ON TABLE bin_table    IS 'BIN routing table mapping card ranges to issuers';
