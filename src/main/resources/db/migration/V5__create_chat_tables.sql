CREATE TABLE chat_messages (
    id             VARCHAR(36)  NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    version        BIGINT       NOT NULL DEFAULT 0,
    created_at     TIMESTAMP             DEFAULT NOW(),
    created_by     VARCHAR(255),
    updated_at     TIMESTAMP,
    updated_by     VARCHAR(255),
    ride_id        VARCHAR(36)  NOT NULL,
    sender_id      VARCHAR(36)  NOT NULL,
    recipient_id   VARCHAR(36)  NOT NULL,
    content        TEXT         NOT NULL,
    message_status VARCHAR(20)  NOT NULL DEFAULT 'SENT',
    CONSTRAINT pk_chat_messages        PRIMARY KEY (id),
    CONSTRAINT fk_chat_messages_sender    FOREIGN KEY (sender_id)    REFERENCES users(id),
    CONSTRAINT fk_chat_messages_recipient FOREIGN KEY (recipient_id) REFERENCES users(id)
);

CREATE INDEX idx_chat_messages_ride_id   ON chat_messages(ride_id);
CREATE INDEX idx_chat_messages_sender    ON chat_messages(sender_id);
CREATE INDEX idx_chat_messages_recipient ON chat_messages(recipient_id);
