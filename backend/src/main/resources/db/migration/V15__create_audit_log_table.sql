CREATE TABLE audit_log
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    event_type  VARCHAR(40)  NOT NULL,
    target_type VARCHAR(30)  NOT NULL,
    target_id   BIGINT       NOT NULL,
    actor_id    BIGINT       NOT NULL,
    reason      VARCHAR(500) NOT NULL,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_audit_log PRIMARY KEY (id),
    INDEX idx_audit_log_target (target_type, target_id),
    INDEX idx_audit_log_actor (actor_id),
    CONSTRAINT fk_audit_log_actor
        FOREIGN KEY (actor_id) REFERENCES app_user (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_audit_log_event_type CHECK (
        event_type IN (
            'GOODS_RECEIPT_CANCELLED',
            'INVOICE_CANCELLED',
            'ACCRUAL_REVERSED',
            'ACCRUAL_REPOSTED'
        )
    ),
    CONSTRAINT ck_audit_log_target_type
        CHECK (target_type IN ('GOODS_RECEIPT', 'INVOICE', 'ACCRUAL_ENTRY')),
    CONSTRAINT ck_audit_log_reason CHECK (CHAR_LENGTH(TRIM(reason)) > 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '핵심 업무 상태 변경 감사 로그';
