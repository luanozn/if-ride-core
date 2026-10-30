CREATE TABLE audit_logs
(
    id          VARCHAR(36) PRIMARY KEY,
    action      VARCHAR(100) NOT NULL,
    target_id   VARCHAR(36)  NOT NULL,
    target_type VARCHAR(50)  NOT NULL,
    author_id    VARCHAR(36)  NOT NULL,
    ip_address  VARCHAR(45),

    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_target ON audit_logs (target_id);
CREATE INDEX idx_audit_logs_actor ON audit_logs (author_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at DESC);