CREATE TABLE tokens
(
    token   varchar(36) PRIMARY KEY UNIQUE NOT NULL,
    expires TIMESTAMPTZ                    NOT NULL,
    type    varchar                        not null,
    user_id varchar                        not null,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users (id)

)