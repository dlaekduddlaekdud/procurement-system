CREATE TABLE close_run
(
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    close_period_id BIGINT        NOT NULL,
    attempt_no      INT           NOT NULL,
    status          VARCHAR(20)   NOT NULL,
    trigger_type    VARCHAR(20)   NOT NULL,
    started_at      DATETIME(6)   NOT NULL,
    completed_at    DATETIME(6)   NULL,
    failure_message VARCHAR(1000) NULL,
    requested_by    BIGINT        NULL,
    version         BIGINT        NOT NULL DEFAULT 0,
    created_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_close_run PRIMARY KEY (id),
    CONSTRAINT uk_close_run_period_attempt UNIQUE (close_period_id, attempt_no),
    INDEX idx_close_run_status (status),
    INDEX idx_close_run_requested_by (requested_by),
    CONSTRAINT fk_close_run_period
        FOREIGN KEY (close_period_id) REFERENCES close_period (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_close_run_requester
        FOREIGN KEY (requested_by) REFERENCES app_user (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_close_run_attempt CHECK (attempt_no > 0),
    CONSTRAINT ck_close_run_status
        CHECK (status IN ('RUNNING', 'SUCCESS', 'FAILED', 'SKIPPED')),
    CONSTRAINT ck_close_run_trigger_type
        CHECK (trigger_type IN ('MANUAL', 'SCHEDULED')),
    CONSTRAINT ck_close_run_requester CHECK (
        (trigger_type = 'MANUAL' AND requested_by IS NOT NULL)
        OR (trigger_type = 'SCHEDULED' AND requested_by IS NULL)
    ),
    CONSTRAINT ck_close_run_completion CHECK (
        (status = 'RUNNING' AND completed_at IS NULL AND failure_message IS NULL)
        OR (status IN ('SUCCESS', 'SKIPPED') AND completed_at IS NOT NULL AND failure_message IS NULL)
        OR (status = 'FAILED' AND completed_at IS NOT NULL AND failure_message IS NOT NULL)
    )
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '월 마감 실행 이력';
