CREATE TABLE users(
    id varchar(36) PRIMARY KEY UNIQUE NOT NULL,
    first_name varchar not null,
    last_name varchar,
    email varchar not null unique,
    password varchar not null,
    role varchar not null
);