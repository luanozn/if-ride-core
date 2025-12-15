CREATE TABLE users(
    id varchar(36) PRIMARY KEY UNIQUE NOT NULL,
    name varchar not null,
    email varchar not null unique,
    password varchar not null,
    email_verified boolean,
    role varchar not null,
    created_at TIMESTAMPTZ not null,
    updated_at TIMESTAMPTZ not null,
    deleted boolean
);