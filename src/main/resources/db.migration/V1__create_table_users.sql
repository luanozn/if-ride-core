CREATE TABLE users(
    id varchar(36) PRIMARY KEY UNIQUE NOT NULL,
    firstName varchar not null,
    lastName varchar,
    email varchar not null unique,
    password varchar not null,
    role varchar not null
);