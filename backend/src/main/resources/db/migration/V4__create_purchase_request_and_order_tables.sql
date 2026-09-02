CREATE TABLE purchase_request
(
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    request_number   VARCHAR(30)   NOT NULL,
    requester_id     BIGINT        NOT NULL,
    department_id    BIGINT        NOT NULL,
    title            VARCHAR(200)  NOT NULL,
    purpose          VARCHAR(1000) NULL,
    status           VARCHAR(30)   NOT NULL DEFAULT 'DRAFT',
    request_date     DATE          NOT NULL,
    needed_date      DATE          NULL,
    submitted_at     DATETIME(6)   NULL,
    approved_by      BIGINT        NULL,
    approved_at      DATETIME(6)   NULL,
    rejection_reason VARCHAR(500)  NULL,
    version          BIGINT        NOT NULL DEFAULT 0,
    created_at       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_purchase_request PRIMARY KEY (id),
    CONSTRAINT uk_purchase_request_number UNIQUE (request_number),
    INDEX idx_purchase_request_requester_id (requester_id),
    INDEX idx_purchase_request_department_id (department_id),
    INDEX idx_purchase_request_status (status),
    INDEX idx_purchase_request_approved_by (approved_by),
    CONSTRAINT fk_purchase_request_requester
        FOREIGN KEY (requester_id) REFERENCES app_user (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_purchase_request_department
        FOREIGN KEY (department_id) REFERENCES department (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_purchase_request_approver
        FOREIGN KEY (approved_by) REFERENCES app_user (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_purchase_request_status
        CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT ck_purchase_request_self_approval
        CHECK (approved_by IS NULL OR approved_by <> requester_id),
    CONSTRAINT ck_purchase_request_needed_date
        CHECK (needed_date IS NULL OR needed_date >= request_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '구매요청 헤더';

CREATE TABLE purchase_request_line
(
    id                   BIGINT         NOT NULL AUTO_INCREMENT,
    purchase_request_id  BIGINT         NOT NULL,
    line_number          INT            NOT NULL,
    item_id              BIGINT         NOT NULL,
    quantity             DECIMAL(19, 3) NOT NULL,
    unit                 VARCHAR(20)    NOT NULL,
    estimated_unit_price DECIMAL(19, 2) NOT NULL,
    estimated_amount     DECIMAL(19, 2) NOT NULL,
    description          VARCHAR(500)   NULL,
    created_at           DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at           DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_purchase_request_line PRIMARY KEY (id),
    CONSTRAINT uk_purchase_request_line_number UNIQUE (purchase_request_id, line_number),
    INDEX idx_purchase_request_line_item_id (item_id),
    CONSTRAINT fk_purchase_request_line_request
        FOREIGN KEY (purchase_request_id) REFERENCES purchase_request (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_purchase_request_line_item
        FOREIGN KEY (item_id) REFERENCES item (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_purchase_request_line_number CHECK (line_number > 0),
    CONSTRAINT ck_purchase_request_line_quantity CHECK (quantity > 0),
    CONSTRAINT ck_purchase_request_line_unit_price CHECK (estimated_unit_price >= 0),
    CONSTRAINT ck_purchase_request_line_amount CHECK (estimated_amount >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '구매요청 품목 라인';

CREATE TABLE purchase_order
(
    id                     BIGINT       NOT NULL AUTO_INCREMENT,
    order_number           VARCHAR(30)  NOT NULL,
    purchase_request_id    BIGINT       NOT NULL,
    vendor_id              BIGINT       NOT NULL,
    buyer_id               BIGINT       NOT NULL,
    warehouse_id           BIGINT       NOT NULL,
    status                 VARCHAR(30)  NOT NULL DEFAULT 'CREATED',
    order_date             DATE         NOT NULL,
    expected_delivery_date DATE         NULL,
    currency               CHAR(3)      NOT NULL DEFAULT 'KRW',
    version                BIGINT       NOT NULL DEFAULT 0,
    created_at             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_purchase_order PRIMARY KEY (id),
    CONSTRAINT uk_purchase_order_number UNIQUE (order_number),
    CONSTRAINT uk_purchase_order_request UNIQUE (purchase_request_id),
    INDEX idx_purchase_order_vendor_id (vendor_id),
    INDEX idx_purchase_order_buyer_id (buyer_id),
    INDEX idx_purchase_order_warehouse_id (warehouse_id),
    INDEX idx_purchase_order_status (status),
    CONSTRAINT fk_purchase_order_request
        FOREIGN KEY (purchase_request_id) REFERENCES purchase_request (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_purchase_order_vendor
        FOREIGN KEY (vendor_id) REFERENCES vendor (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_purchase_order_buyer
        FOREIGN KEY (buyer_id) REFERENCES app_user (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_purchase_order_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouse (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_purchase_order_status
        CHECK (status IN ('CREATED', 'SENT', 'PARTIALLY_RECEIVED', 'RECEIVED', 'CANCELLED')),
    CONSTRAINT ck_purchase_order_delivery_date
        CHECK (expected_delivery_date IS NULL OR expected_delivery_date >= order_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '발주 헤더';

CREATE TABLE purchase_order_line
(
    id                       BIGINT         NOT NULL AUTO_INCREMENT,
    purchase_order_id        BIGINT         NOT NULL,
    purchase_request_line_id BIGINT         NOT NULL,
    line_number              INT            NOT NULL,
    item_id                  BIGINT         NOT NULL,
    quantity                 DECIMAL(19, 3) NOT NULL,
    unit                     VARCHAR(20)    NOT NULL,
    unit_price               DECIMAL(19, 2) NOT NULL,
    supply_amount            DECIMAL(19, 2) NOT NULL,
    tax_amount               DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    total_amount             DECIMAL(19, 2) NOT NULL,
    expected_delivery_date   DATE           NULL,
    created_at               DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at               DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_purchase_order_line PRIMARY KEY (id),
    CONSTRAINT uk_purchase_order_line_number UNIQUE (purchase_order_id, line_number),
    CONSTRAINT uk_purchase_order_line_request_line UNIQUE (purchase_request_line_id),
    INDEX idx_purchase_order_line_item_id (item_id),
    CONSTRAINT fk_purchase_order_line_order
        FOREIGN KEY (purchase_order_id) REFERENCES purchase_order (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_purchase_order_line_request_line
        FOREIGN KEY (purchase_request_line_id) REFERENCES purchase_request_line (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_purchase_order_line_item
        FOREIGN KEY (item_id) REFERENCES item (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_purchase_order_line_number CHECK (line_number > 0),
    CONSTRAINT ck_purchase_order_line_quantity CHECK (quantity > 0),
    CONSTRAINT ck_purchase_order_line_unit_price CHECK (unit_price >= 0),
    CONSTRAINT ck_purchase_order_line_supply_amount CHECK (supply_amount >= 0),
    CONSTRAINT ck_purchase_order_line_tax_amount CHECK (tax_amount >= 0),
    CONSTRAINT ck_purchase_order_line_total_amount CHECK (total_amount >= 0),
    CONSTRAINT ck_purchase_order_line_amount_sum CHECK (total_amount = supply_amount + tax_amount)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '발주 품목 라인';
