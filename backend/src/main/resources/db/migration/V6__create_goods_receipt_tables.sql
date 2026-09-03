CREATE TABLE goods_receipt
(
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    receipt_number    VARCHAR(30)  NOT NULL,
    purchase_order_id BIGINT       NOT NULL,
    warehouse_id      BIGINT       NOT NULL,
    received_by       BIGINT       NOT NULL,
    status            VARCHAR(30)  NOT NULL DEFAULT 'POSTED',
    posting_date      DATE         NOT NULL,
    received_at       DATETIME(6)  NOT NULL,
    version           BIGINT       NOT NULL DEFAULT 0,
    created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_goods_receipt PRIMARY KEY (id),
    CONSTRAINT uk_goods_receipt_number UNIQUE (receipt_number),
    INDEX idx_goods_receipt_purchase_order (purchase_order_id),
    INDEX idx_goods_receipt_warehouse (warehouse_id),
    INDEX idx_goods_receipt_received_by (received_by),
    INDEX idx_goods_receipt_posting_date (posting_date),
    CONSTRAINT fk_goods_receipt_purchase_order
        FOREIGN KEY (purchase_order_id) REFERENCES purchase_order (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_goods_receipt_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouse (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_goods_receipt_receiver
        FOREIGN KEY (received_by) REFERENCES app_user (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_goods_receipt_status CHECK (status IN ('POSTED', 'CANCELLED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '입고 헤더';

CREATE TABLE goods_receipt_line
(
    id                     BIGINT         NOT NULL AUTO_INCREMENT,
    goods_receipt_id       BIGINT         NOT NULL,
    purchase_order_line_id BIGINT         NOT NULL,
    line_number            INT            NOT NULL,
    quantity               DECIMAL(19, 3) NOT NULL,
    created_at             DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at             DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_goods_receipt_line PRIMARY KEY (id),
    CONSTRAINT uk_goods_receipt_line_number UNIQUE (goods_receipt_id, line_number),
    CONSTRAINT uk_goods_receipt_order_line UNIQUE (goods_receipt_id, purchase_order_line_id),
    INDEX idx_goods_receipt_line_order_line (purchase_order_line_id),
    CONSTRAINT fk_goods_receipt_line_receipt
        FOREIGN KEY (goods_receipt_id) REFERENCES goods_receipt (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_goods_receipt_line_order_line
        FOREIGN KEY (purchase_order_line_id) REFERENCES purchase_order_line (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_goods_receipt_line_number CHECK (line_number > 0),
    CONSTRAINT ck_goods_receipt_line_quantity CHECK (quantity > 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '입고 품목 라인';
