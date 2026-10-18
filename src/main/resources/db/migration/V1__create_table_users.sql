CREATE TABLE users
(
    id             VARCHAR(36) PRIMARY KEY,
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',

    name           VARCHAR(255) NOT NULL,
    email          VARCHAR(255) NOT NULL,
    cpf_encrypted  VARCHAR(255) NOT NULL,
    password       VARCHAR(255) NOT NULL,
    email_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    role           VARCHAR(50)  NOT NULL,

    created_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     VARCHAR(255),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by     VARCHAR(255),
    version        BIGINT       NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_users_email ON users (email);
CREATE UNIQUE INDEX uk_users_cpf_encrypted ON users (cpf_encrypted);
CREATE INDEX idx_users_name_prefix ON users (name varchar_pattern_ops);
CREATE INDEX idx_users_status on users (status);
