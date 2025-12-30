CREATE TABLE driver_applications
(
    id               varchar(36) PRIMARY KEY UNIQUE NOT NULL,
    user_id          varchar(36) references users (id),
    status           varchar                        not null,
    cnh_number       varchar,
    cnh_category     varchar,
    cnh_expiration   TIMESTAMPTZ                    not null,
    rejection_reason varchar,
    reviewed_by      varchar(36) references users (id),
    created_at       TIMESTAMPTZ                    not null,
    updated_at       TIMESTAMPTZ                    not null,
    deleted          boolean
);

CREATE TABLE drivers
(
    id             varchar(36) PRIMARY KEY UNIQUE NOT NULL,
    user_id        varchar(36) references users (id),
    cnh_number     varchar,
    cnh_category   varchar,
    cnh_expiration TIMESTAMPTZ                    not null,
    created_at     TIMESTAMPTZ                    not null,
    updated_at     TIMESTAMPTZ                    not null,
    deleted        boolean
)