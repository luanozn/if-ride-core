CREATE TABLE users(
    id varchar(36) PRIMARY KEY UNIQUE NOT NULL,
    name varchar not null,
    email varchar not null unique,
    password varchar not null,
    email_verified boolean,
    role varchar not null
);