ALTER TABLE accrual_entry
    ADD COLUMN invoice_line_id BIGINT NULL AFTER goods_receipt_line_id,
    ADD INDEX idx_accrual_entry_invoice_line (invoice_line_id),
    ADD CONSTRAINT fk_accrual_entry_invoice_line
        FOREIGN KEY (invoice_line_id) REFERENCES invoice_line (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    ADD CONSTRAINT uk_accrual_entry_invoice_line_type UNIQUE (invoice_line_id, entry_type),
    ADD CONSTRAINT uk_accrual_entry_reversal_of UNIQUE (reversal_of_id),
    DROP CHECK ck_accrual_entry_type,
    ADD CONSTRAINT ck_accrual_entry_type
        CHECK (entry_type IN ('GR_ACCRUAL', 'INVOICE_MATCH', 'CANCEL_OFFSET', 'REVERSAL')),
    ADD CONSTRAINT ck_accrual_entry_source CHECK (
        (entry_type = 'GR_ACCRUAL'
            AND goods_receipt_line_id IS NOT NULL
            AND invoice_line_id IS NULL
            AND reversal_of_id IS NULL)
        OR (entry_type = 'INVOICE_MATCH'
            AND goods_receipt_line_id IS NULL
            AND invoice_line_id IS NOT NULL
            AND reversal_of_id IS NULL)
        OR (entry_type IN ('CANCEL_OFFSET', 'REVERSAL')
            AND goods_receipt_line_id IS NULL
            AND invoice_line_id IS NULL
            AND reversal_of_id IS NOT NULL)
    );
