CREATE TABLE accrual_entry
(
    id                     BIGINT         NOT NULL AUTO_INCREMENT,
    entry_number           VARCHAR(40)    NOT NULL,
    entry_type             VARCHAR(30)    NOT NULL,
    purchase_order_line_id BIGINT         NOT NULL,
    goods_receipt_line_id  BIGINT         NULL,
    amount                 DECIMAL(19, 2) NOT NULL,
    currency               CHAR(3)        NOT NULL,
    posting_date           DATE           NOT NULL,
    period                 CHAR(6)        NOT NULL,
    created_by             BIGINT         NOT NULL,
    reversal_of_id         BIGINT         NULL,
    created_at             DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_accrual_entry PRIMARY KEY (id),
    CONSTRAINT uk_accrual_entry_number UNIQUE (entry_number),
    CONSTRAINT uk_accrual_entry_receipt_line_type UNIQUE (goods_receipt_line_id, entry_type),
    INDEX idx_accrual_entry_order_line (purchase_order_line_id),
    INDEX idx_accrual_entry_period (period),
    INDEX idx_accrual_entry_created_by (created_by),
    INDEX idx_accrual_entry_reversal_of (reversal_of_id),
    CONSTRAINT fk_accrual_entry_order_line
        FOREIGN KEY (purchase_order_line_id) REFERENCES purchase_order_line (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_accrual_entry_receipt_line
        FOREIGN KEY (goods_receipt_line_id) REFERENCES goods_receipt_line (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_accrual_entry_creator
        FOREIGN KEY (created_by) REFERENCES app_user (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_accrual_entry_reversal
        FOREIGN KEY (reversal_of_id) REFERENCES accrual_entry (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_accrual_entry_type
        CHECK (entry_type IN ('GR_ACCRUAL', 'INVOICE_MATCH', 'REVERSAL')),
    CONSTRAINT ck_accrual_entry_period
        CHECK (period REGEXP '^[0-9]{6}$')
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '미착 원장';
