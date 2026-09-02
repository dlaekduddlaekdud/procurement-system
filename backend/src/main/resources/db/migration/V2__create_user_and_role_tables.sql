CREATE TABLE app_user
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(100) NOT NULL,
    department_id BIGINT       NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_app_user PRIMARY KEY (id),
    CONSTRAINT uk_app_user_email UNIQUE (email),
    INDEX idx_app_user_department_id (department_id),
    CONSTRAINT fk_app_user_department
        FOREIGN KEY (department_id) REFERENCES department (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '애플리케이션 사용자';

CREATE TABLE `role`
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(30)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_role PRIMARY KEY (id),
    CONSTRAINT uk_role_code UNIQUE (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '사용자 역할';

CREATE TABLE user_role
(
    app_user_id BIGINT      NOT NULL,
    role_id     BIGINT      NOT NULL,
    assigned_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_user_role PRIMARY KEY (app_user_id, role_id),
    INDEX idx_user_role_role_id (role_id),
    CONSTRAINT fk_user_role_app_user
        FOREIGN KEY (app_user_id) REFERENCES app_user (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_user_role_role
        FOREIGN KEY (role_id) REFERENCES `role` (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '사용자별 역할';
