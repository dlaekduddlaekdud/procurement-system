CREATE TABLE department
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(30)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_department PRIMARY KEY (id),
    CONSTRAINT uk_department_code UNIQUE (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '부서 기준정보';

CREATE TABLE warehouse
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    code       VARCHAR(30)  NOT NULL,
    name       VARCHAR(100) NOT NULL,
    address    VARCHAR(255) NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_warehouse PRIMARY KEY (id),
    CONSTRAINT uk_warehouse_code UNIQUE (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '창고 기준정보';

CREATE TABLE item
(
    id                  BIGINT         NOT NULL AUTO_INCREMENT,
    code                VARCHAR(50)    NOT NULL,
    name                VARCHAR(150)   NOT NULL,
    specification       VARCHAR(255)   NULL,
    unit                VARCHAR(20)    NOT NULL,
    standard_unit_price DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    active              BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at          DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_item PRIMARY KEY (id),
    CONSTRAINT uk_item_code UNIQUE (code),
    CONSTRAINT ck_item_standard_unit_price_non_negative CHECK (standard_unit_price >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '품목 기준정보';

CREATE TABLE vendor
(
    id                           BIGINT       NOT NULL AUTO_INCREMENT,
    code                         VARCHAR(30)  NOT NULL,
    name                         VARCHAR(150) NOT NULL,
    business_registration_number VARCHAR(30)  NOT NULL,
    representative_name          VARCHAR(100) NULL,
    contact_name                 VARCHAR(100) NULL,
    contact_email                VARCHAR(255) NULL,
    contact_phone                VARCHAR(30)  NULL,
    address                      VARCHAR(255) NULL,
    active                       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at                   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_vendor PRIMARY KEY (id),
    CONSTRAINT uk_vendor_code UNIQUE (code),
    CONSTRAINT uk_vendor_business_registration_number UNIQUE (business_registration_number)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '공급업체 기준정보';
