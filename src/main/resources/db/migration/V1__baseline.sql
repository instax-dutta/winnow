-- ---------------------------------------------------------------------------
-- The winnow baseline schema.
--
-- The schema of the application. Every table is utf8mb4_0900_ai_ci. Every tenant-owned
-- table carries owner_user_id, the four audit columns and a deleted_at soft-delete marker.
--
-- No secret is stored in this schema. A credential is referenced by key; the
-- value lives in the secret store. See docs/SECURITY.md.
-- ---------------------------------------------------------------------------

-- ---------------------------------------------------------------- users

CREATE TABLE app_user (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username          VARCHAR(150)    NOT NULL,
    password_hash     VARCHAR(100)    NOT NULL,
    email             VARCHAR(254)    NULL,
    employee_id       VARCHAR(64)     NULL,
    status            VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE',
    failed_login_count INT UNSIGNED   NOT NULL DEFAULT 0,
    locked_until      DATETIME(6)     NULL,
    last_login_at     DATETIME(6)     NULL,
    created_at        DATETIME(6)     NOT NULL,
    created_by        BIGINT UNSIGNED NULL,
    updated_at        DATETIME(6)     NOT NULL,
    updated_by        BIGINT UNSIGNED NULL,
    deleted_at        DATETIME(6)     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_user_username (username),
    UNIQUE KEY uk_app_user_employee_id (employee_id),
    KEY idx_app_user_status (status),
    KEY idx_app_user_deleted_at (deleted_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE app_role (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code       VARCHAR(50)     NOT NULL,
    name       VARCHAR(150)    NOT NULL,
    created_at DATETIME(6)     NOT NULL,
    updated_at DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_role_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE app_user_role (
    user_id    BIGINT UNSIGNED NOT NULL,
    role_id    BIGINT UNSIGNED NOT NULL,
    granted_at DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id, role_id),
    KEY idx_app_user_role_role (role_id),
    CONSTRAINT fk_app_user_role_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_app_user_role_role FOREIGN KEY (role_id) REFERENCES app_role (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------- clients

CREATE TABLE client_group (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code          VARCHAR(32)     NOT NULL,
    name          VARCHAR(255)    NOT NULL,
    owner_user_id BIGINT UNSIGNED NULL,
    created_at    DATETIME(6)     NOT NULL,
    created_by    BIGINT UNSIGNED NULL,
    updated_at    DATETIME(6)     NOT NULL,
    updated_by    BIGINT UNSIGNED NULL,
    deleted_at    DATETIME(6)     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_client_group_code (code),
    KEY idx_client_group_owner (owner_user_id),
    KEY idx_client_group_deleted_at (deleted_at),
    CONSTRAINT fk_client_group_owner FOREIGN KEY (owner_user_id) REFERENCES app_user (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE client (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name          VARCHAR(255)    NOT NULL,
    email         VARCHAR(254)    NOT NULL,
    web_link      VARCHAR(2048)   NULL,
    group_id      BIGINT UNSIGNED NULL,
    owner_user_id BIGINT UNSIGNED NOT NULL,
    created_at    DATETIME(6)     NOT NULL,
    created_by    BIGINT UNSIGNED NULL,
    updated_at    DATETIME(6)     NOT NULL,
    updated_by    BIGINT UNSIGNED NULL,
    deleted_at    DATETIME(6)     NULL,
    PRIMARY KEY (id),
    KEY idx_client_group (group_id),
    KEY idx_client_owner (owner_user_id),
    KEY idx_client_email (email),
    KEY idx_client_deleted_at (deleted_at),
    CONSTRAINT fk_client_group FOREIGN KEY (group_id) REFERENCES client_group (id) ON DELETE RESTRICT,
    CONSTRAINT fk_client_owner FOREIGN KEY (owner_user_id) REFERENCES app_user (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------- templates

CREATE TABLE email_template (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name          VARCHAR(255)    NOT NULL,
    subject       VARCHAR(998)    NOT NULL,
    content_html  MEDIUMTEXT      NOT NULL,
    status        VARCHAR(32)     NOT NULL DEFAULT 'DRAFT',
    version       INT UNSIGNED    NOT NULL DEFAULT 1,
    owner_user_id BIGINT UNSIGNED NOT NULL,
    approved_by   BIGINT UNSIGNED NULL,
    approved_at   DATETIME(6)     NULL,
    created_at    DATETIME(6)     NOT NULL,
    created_by    BIGINT UNSIGNED NULL,
    updated_at    DATETIME(6)     NOT NULL,
    updated_by    BIGINT UNSIGNED NULL,
    deleted_at    DATETIME(6)     NULL,
    PRIMARY KEY (id),
    KEY idx_email_template_owner (owner_user_id),
    KEY idx_email_template_status (status),
    KEY idx_email_template_deleted_at (deleted_at),
    CONSTRAINT fk_email_template_owner FOREIGN KEY (owner_user_id) REFERENCES app_user (id) ON DELETE RESTRICT,
    CONSTRAINT fk_email_template_approved_by FOREIGN KEY (approved_by) REFERENCES app_user (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------- sending accounts

-- A credential is never stored here. secret_ref is a key into the secret store.
CREATE TABLE smtp_account (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    email         VARCHAR(254)    NOT NULL,
    protocol      VARCHAR(16)     NOT NULL DEFAULT 'SMTP',
    host          VARCHAR(255)    NOT NULL,
    port          INT UNSIGNED    NOT NULL,
    username      VARCHAR(254)    NOT NULL,
    auth_method   VARCHAR(16)     NOT NULL DEFAULT 'PLAIN',
    enabled       BOOLEAN         NOT NULL DEFAULT TRUE,
    secret_ref    VARCHAR(255)    NULL,
    owner_user_id BIGINT UNSIGNED NOT NULL,
    created_at    DATETIME(6)     NOT NULL,
    created_by    BIGINT UNSIGNED NULL,
    updated_at    DATETIME(6)     NOT NULL,
    updated_by    BIGINT UNSIGNED NULL,
    deleted_at    DATETIME(6)     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_smtp_account_email (email),
    KEY idx_smtp_account_owner (owner_user_id),
    KEY idx_smtp_account_deleted_at (deleted_at),
    CONSTRAINT ck_smtp_account_port CHECK (port BETWEEN 1 AND 65535),
    CONSTRAINT fk_smtp_account_owner FOREIGN KEY (owner_user_id) REFERENCES app_user (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE gmail_account (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    email            VARCHAR(254)    NOT NULL,
    oauth_client_id  VARCHAR(255)    NOT NULL,
    secret_ref       VARCHAR(255)    NULL,
    refresh_token_ref VARCHAR(255)   NULL,
    scopes           VARCHAR(512)    NULL,
    enabled          BOOLEAN         NOT NULL DEFAULT TRUE,
    owner_user_id    BIGINT UNSIGNED NOT NULL,
    created_at       DATETIME(6)     NOT NULL,
    created_by       BIGINT UNSIGNED NULL,
    updated_at       DATETIME(6)     NOT NULL,
    updated_by       BIGINT UNSIGNED NULL,
    deleted_at       DATETIME(6)     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_gmail_account_email (email),
    KEY idx_gmail_account_owner (owner_user_id),
    KEY idx_gmail_account_deleted_at (deleted_at),
    CONSTRAINT fk_gmail_account_owner FOREIGN KEY (owner_user_id) REFERENCES app_user (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------- campaigns

CREATE TABLE campaign (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    from_account_id   BIGINT UNSIGNED NULL,
    from_account_kind VARCHAR(16)     NOT NULL,
    target_type       VARCHAR(16)     NOT NULL,
    template_id       BIGINT UNSIGNED NULL,
    recipient_count   INT UNSIGNED    NOT NULL DEFAULT 0,
    success_count     INT UNSIGNED    NOT NULL DEFAULT 0,
    failure_count     INT UNSIGNED    NOT NULL DEFAULT 0,
    status            VARCHAR(32)     NOT NULL DEFAULT 'QUEUED',
    error_detail      TEXT            NULL,
    started_at        DATETIME(6)     NULL,
    finished_at       DATETIME(6)     NULL,
    owner_user_id     BIGINT UNSIGNED NOT NULL,
    created_at        DATETIME(6)     NOT NULL,
    created_by        BIGINT UNSIGNED NULL,
    updated_at        DATETIME(6)     NOT NULL,
    updated_by        BIGINT UNSIGNED NULL,
    deleted_at        DATETIME(6)     NULL,
    PRIMARY KEY (id),
    KEY idx_campaign_owner (owner_user_id),
    KEY idx_campaign_deleted_at (deleted_at),
    KEY idx_campaign_from (from_account_id),
    KEY idx_campaign_template (template_id),
    KEY idx_campaign_status_created (status, created_at),
    CONSTRAINT fk_campaign_template FOREIGN KEY (template_id) REFERENCES email_template (id) ON DELETE SET NULL,
    CONSTRAINT fk_campaign_owner FOREIGN KEY (owner_user_id) REFERENCES app_user (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE delivery (
    id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    campaign_id        BIGINT UNSIGNED NOT NULL,
    recipient_email    VARCHAR(254)    NOT NULL,
    template_id        BIGINT UNSIGNED NULL,
    status             VARCHAR(32)     NOT NULL DEFAULT 'QUEUED',
    provider_message_id VARCHAR(255)   NULL,
    error_detail       TEXT            NULL,
    sent_at            DATETIME(6)     NULL,
    created_at         DATETIME(6)     NOT NULL,
    created_by         BIGINT UNSIGNED NULL,
    updated_at         DATETIME(6)     NOT NULL,
    updated_by         BIGINT UNSIGNED NULL,
    PRIMARY KEY (id),
    KEY idx_delivery_campaign (campaign_id),
    KEY idx_delivery_recipient (recipient_email),
    KEY idx_delivery_template (template_id),
    KEY idx_delivery_status (status),
    CONSTRAINT fk_delivery_campaign FOREIGN KEY (campaign_id) REFERENCES campaign (id) ON DELETE CASCADE,
    CONSTRAINT fk_delivery_template FOREIGN KEY (template_id) REFERENCES email_template (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- The scheduled outbox. A row is claimed by a conditional status update, so a
-- row is never dispatched twice.
CREATE TABLE outbox (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    from_account_id BIGINT UNSIGNED NULL,
    to_email       VARCHAR(254)    NOT NULL,
    draft_id       VARCHAR(255)    NULL,
    subject        VARCHAR(998)    NULL,
    body_html      MEDIUMTEXT      NULL,
    scheduled_at   DATETIME(6)     NOT NULL,
    status         VARCHAR(32)     NOT NULL DEFAULT 'SCHEDULED',
    attempt_count  INT UNSIGNED    NOT NULL DEFAULT 0,
    error_message  TEXT            NULL,
    owner_user_id  BIGINT UNSIGNED NOT NULL,
    created_at     DATETIME(6)     NOT NULL,
    created_by     BIGINT UNSIGNED NULL,
    updated_at     DATETIME(6)     NOT NULL,
    updated_by     BIGINT UNSIGNED NULL,
    deleted_at     DATETIME(6)     NULL,
    PRIMARY KEY (id),
    KEY idx_outbox_status_scheduled (status, scheduled_at),
    KEY idx_outbox_deleted_at (deleted_at),
    KEY idx_outbox_from (from_account_id),
    KEY idx_outbox_to (to_email),
    KEY idx_outbox_owner (owner_user_id),
    CONSTRAINT fk_outbox_owner FOREIGN KEY (owner_user_id) REFERENCES app_user (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------- extraction

CREATE TABLE extraction_run (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    status            VARCHAR(32)     NOT NULL DEFAULT 'QUEUED',
    domain_count      BIGINT UNSIGNED NOT NULL DEFAULT 0,
    source_file_name  VARCHAR(512)    NULL,
    started_at        DATETIME(6)     NOT NULL,
    finished_at       DATETIME(6)     NULL,
    error_detail      TEXT            NULL,
    owner_user_id     BIGINT UNSIGNED NOT NULL,
    created_at        DATETIME(6)     NOT NULL,
    created_by        BIGINT UNSIGNED NULL,
    updated_at        DATETIME(6)     NOT NULL,
    updated_by        BIGINT UNSIGNED NULL,
    deleted_at        DATETIME(6)     NULL,
    PRIMARY KEY (id),
    KEY idx_extraction_run_owner (owner_user_id),
    KEY idx_extraction_run_deleted_at (deleted_at),
    KEY idx_extraction_run_started (started_at),
    CONSTRAINT fk_extraction_run_owner FOREIGN KEY (owner_user_id) REFERENCES app_user (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE extraction_result (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    run_id     BIGINT UNSIGNED NOT NULL,
    domain     VARCHAR(255)    NOT NULL,
    email      VARCHAR(254)    NOT NULL,
    email_hash BINARY(32)      NOT NULL,
    source_row INT UNSIGNED    NULL,
    created_at DATETIME(6)     NOT NULL,
    created_by BIGINT UNSIGNED NULL,
    updated_at DATETIME(6)     NOT NULL,
    updated_by BIGINT UNSIGNED NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_extraction_result_run_email (run_id, email_hash),
    KEY idx_extraction_result_run (run_id),
    KEY idx_extraction_result_domain (domain),
    CONSTRAINT fk_extraction_result_run FOREIGN KEY (run_id) REFERENCES extraction_run (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------- inbox

CREATE TABLE inbox_message (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    account_id          BIGINT UNSIGNED NULL,
    provider_message_id VARCHAR(255)    NOT NULL,
    from_address        VARCHAR(254)    NOT NULL,
    subject             VARCHAR(998)    NULL,
    body_html           MEDIUMTEXT      NULL,
    received_at         DATETIME(6)     NOT NULL,
    is_read             BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at          DATETIME(6)     NOT NULL,
    created_by          BIGINT UNSIGNED NULL,
    updated_at          DATETIME(6)     NOT NULL,
    updated_by          BIGINT UNSIGNED NULL,
    deleted_at          DATETIME(6)     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inbox_account_message (account_id, provider_message_id),
    KEY idx_inbox_received (received_at),
    KEY idx_inbox_from (from_address),
    KEY idx_inbox_deleted_at (deleted_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------- seed roles

INSERT INTO app_role (code, name, created_at, updated_at) VALUES
    ('ROLE_ADMIN',    'Administrator',   NOW(6), NOW(6)),
    ('ROLE_MANAGER',  'Manager',         NOW(6), NOW(6)),
    ('ROLE_OPERATOR', 'Campaign operator', NOW(6), NOW(6)),
    ('ROLE_VIEWER',   'Read-only viewer', NOW(6), NOW(6));

-- The bootstrap administrator. The password is 'change-me-now' and MUST be changed on
-- first use. The hash is BCrypt, cost 12, prefix 2a, and it really does verify against
-- that password. Setting SEO_BOOTSTRAP_PASSWORD replaces it on the first start.
INSERT INTO app_user (username, password_hash, email, status, created_at, updated_at)
VALUES ('admin', '$2a$12$O0G3lOD0rmcfNlQoCM9do.vE5G1amtvMG0cD/7PmKC8VAsIK1a4s2', 'admin@example.invalid', 'ACTIVE', NOW(6), NOW(6));

INSERT INTO app_user_role (user_id, role_id, granted_at)
SELECT u.id, r.id, NOW(6) FROM app_user u JOIN app_role r ON r.code = 'ROLE_ADMIN'
WHERE u.username = 'admin';
