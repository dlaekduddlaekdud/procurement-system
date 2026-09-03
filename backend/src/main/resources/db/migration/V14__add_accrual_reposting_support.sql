ALTER TABLE accrual_entry
    ADD COLUMN reposting_of_id BIGINT NULL AFTER reversal_of_id,
    ADD INDEX idx_accrual_entry_reposting_of (reposting_of_id),
    ADD CONSTRAINT fk_accrual_entry_reposting
        FOREIGN KEY (reposting_of_id) REFERENCES accrual_entry (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    ADD CONSTRAINT uk_accrual_entry_reposting_of UNIQUE (reposting_of_id),
    DROP CHECK ck_accrual_entry_type,
    ADD CONSTRAINT ck_accrual_entry_type
        CHECK (entry_type IN ('GR_ACCRUAL', 'INVOICE_MATCH', 'CANCEL_OFFSET', 'REVERSAL', 'CORRECTION')),
    DROP CHECK ck_accrual_entry_source,
    ADD CONSTRAINT ck_accrual_entry_source CHECK (
        (entry_type = 'GR_ACCRUAL'
            AND goods_receipt_line_id IS NOT NULL
            AND invoice_line_id IS NULL
            AND reversal_of_id IS NULL
            AND reposting_of_id IS NULL)
        OR (entry_type = 'INVOICE_MATCH'
            AND goods_receipt_line_id IS NULL
            AND invoice_line_id IS NOT NULL
            AND reversal_of_id IS NULL
            AND reposting_of_id IS NULL)
        OR (entry_type IN ('CANCEL_OFFSET', 'REVERSAL')
            AND goods_receipt_line_id IS NULL
            AND invoice_line_id IS NULL
            AND reversal_of_id IS NOT NULL
            AND reposting_of_id IS NULL)
        OR (entry_type = 'CORRECTION'
            AND goods_receipt_line_id IS NULL
            AND invoice_line_id IS NULL
            AND reversal_of_id IS NULL
            AND reposting_of_id IS NOT NULL)
    );
