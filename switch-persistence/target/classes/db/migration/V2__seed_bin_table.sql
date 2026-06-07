-- ============================================================
-- V2 – Seed default BIN routing entries
-- ============================================================

INSERT INTO bin_table (bin_range_start, bin_range_end, issuer_id, issuer_name, active) VALUES
    ('400000', '499999', 'ISSUER_VISA_01',   'Visa Issuer 1',       TRUE),
    ('510000', '559999', 'ISSUER_MC_01',     'Mastercard Issuer 1', TRUE),
    ('340000', '349999', 'ISSUER_AMEX_01',   'Amex Issuer 1',       TRUE),
    ('370000', '379999', 'ISSUER_AMEX_01',   'Amex Issuer 1',       TRUE)
ON CONFLICT DO NOTHING;
