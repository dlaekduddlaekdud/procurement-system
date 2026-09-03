CREATE TABLE invoice
(
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    invoice_number    VARCHAR(50)    NOT NULL,
    purchase_order_id BIGINT         NOT NULL,
    vendor_id         BIGINT         NOT NULL,
    received_by       BIGINT         NOT NULL,
    status            VARCHAR(20)    NOT NULL DEFAULT 'RECEIVED',
    invoice_date      DATE           NOT NULL,
    posting_date      DATE           NOT NULL,
    currency          CHAR(3)        NOT NULL,
    supply_amount     DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    tax_amount        DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    total_amount      DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    version           BIGINT         NOT NULL DEFAULT 0,
    created_at        DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_invoice PRIMARY KEY (id),
    CONSTRAINT uk_invoice_vendor_number UNIQUE (vendor_id, invoice_number),
    INDEX idx_invoice_purchase_order (purchase_order_id),
    INDEX idx_invoice_received_by (received_by),
    INDEX idx_invoice_posting_date (posting_date),
    CONSTRAINT fk_invoice_purchase_order
        FOREIGN KEY (purchase_order_id) REFERENCES purchase_order (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_invoice_vendor
        FOREIGN KEY (vendor_id) REFERENCES vendor (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_invoice_receiver
        FOREIGN KEY (received_by) REFERENCES app_user (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_invoice_status CHECK (status IN ('RECEIVED', 'CANCELLED')),
    CONSTRAINT ck_invoice_supply_amount CHECK (supply_amount >= 0),
    CONSTRAINT ck_invoice_tax_amount CHECK (tax_amount >= 0),
    CONSTRAINT ck_invoice_total_amount CHECK (total_amount = supply_amount + tax_amount)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '송장 헤더';

CREATE TABLE invoice_line
(
    id                     BIGINT         NOT NULL AUTO_INCREMENT,
    invoice_id             BIGINT         NOT NULL,
    purchase_order_line_id BIGINT         NOT NULL,
    line_number            INT            NOT NULL,
    quantity               DECIMAL(19, 3) NOT NULL,
    unit_price             DECIMAL(19, 2) NOT NULL,
    supply_amount          DECIMAL(19, 2) NOT NULL,
    tax_amount             DECIMAL(19, 2) NOT NULL,
    total_amount           DECIMAL(19, 2) NOT NULL,
    created_at             DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at             DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_invoice_line PRIMARY KEY (id),
    CONSTRAINT uk_invoice_line_number UNIQUE (invoice_id, line_number),
    CONSTRAINT uk_invoice_order_line UNIQUE (invoice_id, purchase_order_line_id),
    INDEX idx_invoice_line_order_line (purchase_order_line_id),
    CONSTRAINT fk_invoice_line_invoice
        FOREIGN KEY (invoice_id) REFERENCES invoice (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_invoice_line_order_line
        FOREIGN KEY (purchase_order_line_id) REFERENCES purchase_order_line (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_invoice_line_number CHECK (line_number > 0),
    CONSTRAINT ck_invoice_line_quantity CHECK (quantity > 0),
    CONSTRAINT ck_invoice_line_unit_price CHECK (unit_price >= 0),
    CONSTRAINT ck_invoice_line_supply_amount CHECK (supply_amount >= 0),
    CONSTRAINT ck_invoice_line_tax_amount CHECK (tax_amount >= 0),
    CONSTRAINT ck_invoice_line_total_amount CHECK (total_amount = supply_amount + tax_amount)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '송장 품목 라인';
