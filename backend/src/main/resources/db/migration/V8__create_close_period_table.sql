CREATE TABLE close_period
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    period     CHAR(6)     NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    closed_at  DATETIME(6) NULL,
    version    BIGINT      NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_close_period PRIMARY KEY (id),
    CONSTRAINT uk_close_period_period UNIQUE (period),
    CONSTRAINT ck_close_period_format CHECK (period REGEXP '^[0-9]{6}$'),
    CONSTRAINT ck_close_period_status CHECK (status IN ('OPEN', 'CLOSED')),
    CONSTRAINT ck_close_period_closed_at CHECK (
        (status = 'OPEN' AND closed_at IS NULL)
        OR (status = 'CLOSED' AND closed_at IS NOT NULL)
    )
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '월 마감 기간';
