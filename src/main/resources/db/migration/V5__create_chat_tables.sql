CREATE TABLE conversations (
                               id VARCHAR(36) PRIMARY KEY,
                               ride_id VARCHAR(36) NOT NULL,
                               driver_id VARCHAR(36) NOT NULL,
                               driver_name VARCHAR(255) NOT NULL,
                               passenger_id VARCHAR(36) NOT NULL,
                               passenger_name VARCHAR(255) NOT NULL,
                               last_message TEXT,
                               last_message_at TIMESTAMPTZ,
                               is_blocked BOOLEAN NOT NULL DEFAULT FALSE,
                               created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE chat_messages (
                               id VARCHAR(36) PRIMARY KEY,
                               ride_id VARCHAR(36) NOT NULL,
                               sender_id VARCHAR(36) NOT NULL,
                               sender_name VARCHAR(255) NOT NULL,
                               recipient_id VARCHAR(36),
                               content TEXT NOT NULL,
                               message_status VARCHAR(20) NOT NULL DEFAULT 'SENT',
                               created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_conv_ride ON conversations(ride_id);
CREATE INDEX idx_conv_driver ON conversations(driver_id);
CREATE INDEX idx_conv_passenger ON conversations(passenger_id);
CREATE INDEX idx_msg_ride ON chat_messages(ride_id);
CREATE INDEX idx_msg_sender ON chat_messages(sender_id);
CREATE INDEX idx_msg_recipient ON chat_messages(recipient_id);

CREATE UNIQUE INDEX uk_conv_ride_driver_passenger ON conversations(ride_id, driver_id, passenger_id);