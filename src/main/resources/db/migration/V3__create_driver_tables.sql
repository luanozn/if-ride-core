CREATE TABLE driver_request(
    id varchar(36) PRIMARY KEY UNIQUE NOT NULL,
    user_id varchar(36) references users(id),
    status varchar not null,
    cnh_number varchar,
    cnh_category varchar,
    cnh_expiration TIMESTAMPTZ not null,
    rejection_reason varchar,
    reviewed_by varchar(36) references users(id),
    createdAt TIMESTAMPTZ not null,
    updatedAt TIMESTAMPTZ not null,
    deleted boolean
);

CREATE TABLE driver(
    id varchar(36) PRIMARY KEY UNIQUE NOT NULL,
    user_id varchar(36) references users(id),
    cnh_number varchar,
    cnh_category varchar,
    cnh_expiration TIMESTAMPTZ not null,
    createdAt TIMESTAMPTZ not null,
    updatedAt TIMESTAMPTZ not null,
    deleted boolean
)