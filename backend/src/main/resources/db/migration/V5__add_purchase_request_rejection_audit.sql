ALTER TABLE purchase_request
    ADD COLUMN rejected_by BIGINT NULL AFTER rejection_reason,
    ADD COLUMN rejected_at DATETIME(6) NULL AFTER rejected_by,
    ADD INDEX idx_purchase_request_rejected_by (rejected_by),
    ADD CONSTRAINT fk_purchase_request_rejector
        FOREIGN KEY (rejected_by) REFERENCES app_user (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT;
