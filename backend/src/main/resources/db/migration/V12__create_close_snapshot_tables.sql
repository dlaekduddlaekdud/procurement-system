CREATE TABLE close_accrual_snapshot
(
    id                     BIGINT         NOT NULL AUTO_INCREMENT,
    close_run_id           BIGINT         NOT NULL,
    purchase_order_line_id BIGINT         NOT NULL,
    balance_amount         DECIMAL(19, 2) NOT NULL,
    currency               CHAR(3)        NOT NULL,
    created_at             DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_close_accrual_snapshot PRIMARY KEY (id),
    CONSTRAINT uk_close_accrual_snapshot_run_line UNIQUE (close_run_id, purchase_order_line_id),
    INDEX idx_close_accrual_snapshot_order_line (purchase_order_line_id),
    CONSTRAINT fk_close_accrual_snapshot_run
        FOREIGN KEY (close_run_id) REFERENCES close_run (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_close_accrual_snapshot_order_line
        FOREIGN KEY (purchase_order_line_id) REFERENCES purchase_order_line (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_close_accrual_snapshot_balance CHECK (balance_amount <> 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '월 마감 미착 잔액 스냅샷';

CREATE TABLE close_hold_snapshot
(
    id                     BIGINT         NOT NULL AUTO_INCREMENT,
    close_run_id           BIGINT         NOT NULL,
    match_result_id        BIGINT         NOT NULL,
    purchase_order_line_id BIGINT         NOT NULL,
    status                 VARCHAR(30)    NOT NULL,
    ordered_quantity       DECIMAL(19, 3) NOT NULL,
    received_quantity      DECIMAL(19, 3) NOT NULL,
    invoiced_quantity      DECIMAL(19, 3) NOT NULL,
    ordered_amount         DECIMAL(19, 2) NOT NULL,
    invoiced_amount        DECIMAL(19, 2) NOT NULL,
    created_at             DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_close_hold_snapshot PRIMARY KEY (id),
    CONSTRAINT uk_close_hold_snapshot_run_line UNIQUE (close_run_id, purchase_order_line_id),
    INDEX idx_close_hold_snapshot_match_result (match_result_id),
    INDEX idx_close_hold_snapshot_order_line (purchase_order_line_id),
    CONSTRAINT fk_close_hold_snapshot_run
        FOREIGN KEY (close_run_id) REFERENCES close_run (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_close_hold_snapshot_match_result
        FOREIGN KEY (match_result_id) REFERENCES match_result (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_close_hold_snapshot_order_line
        FOREIGN KEY (purchase_order_line_id) REFERENCES purchase_order_line (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_close_hold_snapshot_status
        CHECK (status IN ('HOLD_QUANTITY', 'HOLD_PRICE')),
    CONSTRAINT ck_close_hold_snapshot_quantities
        CHECK (ordered_quantity >= 0 AND received_quantity >= 0 AND invoiced_quantity >= 0),
    CONSTRAINT ck_close_hold_snapshot_amounts
        CHECK (ordered_amount >= 0 AND invoiced_amount >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '월 마감 미해결 HOLD 스냅샷';
