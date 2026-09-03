CREATE TABLE match_result
(
    id                     BIGINT         NOT NULL AUTO_INCREMENT,
    purchase_order_line_id BIGINT         NOT NULL,
    status                 VARCHAR(30)    NOT NULL,
    ordered_quantity       DECIMAL(19, 3) NOT NULL,
    received_quantity      DECIMAL(19, 3) NOT NULL,
    invoiced_quantity      DECIMAL(19, 3) NOT NULL,
    ordered_amount         DECIMAL(19, 2) NOT NULL,
    invoiced_amount        DECIMAL(19, 2) NOT NULL,
    version                BIGINT         NOT NULL DEFAULT 0,
    created_at             DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at             DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_match_result PRIMARY KEY (id),
    CONSTRAINT uk_match_result_order_line UNIQUE (purchase_order_line_id),
    CONSTRAINT fk_match_result_order_line
        FOREIGN KEY (purchase_order_line_id) REFERENCES purchase_order_line (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_match_result_status
        CHECK (status IN ('MATCHED', 'HOLD_QUANTITY', 'HOLD_PRICE')),
    CONSTRAINT ck_match_result_quantities
        CHECK (ordered_quantity >= 0 AND received_quantity >= 0 AND invoiced_quantity >= 0),
    CONSTRAINT ck_match_result_amounts
        CHECK (ordered_amount >= 0 AND invoiced_amount >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '발주 라인별 현재 3-Way Matching 결과';
