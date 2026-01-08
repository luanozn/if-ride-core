CREATE TABLE driver_applications
(
    id                 VARCHAR(36) PRIMARY KEY,
    status             VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    user_id            VARCHAR(36) NOT NULL,
    application_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    cnh_number         VARCHAR(20) NOT NULL,
    cnh_category       VARCHAR(10) NOT NULL,
    cnh_expiration     DATE        NOT NULL,
    reviewed_by        VARCHAR(36),
    rejection_reason   TEXT,

    created_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(255),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by         VARCHAR(255),

    CONSTRAINT fk_app_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_app_reviewer FOREIGN KEY (reviewed_by) REFERENCES users (id)
);

CREATE TABLE drivers
(
    id             VARCHAR(36) PRIMARY KEY,
    status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    cnh_number     VARCHAR(20) NOT NULL,
    cnh_category   VARCHAR(10) NOT NULL,
    cnh_expiration DATE        NOT NULL,

    created_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     VARCHAR(255),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by     VARCHAR(255),

    CONSTRAINT fk_driver_user FOREIGN KEY (id) REFERENCES users (id)
);

CREATE TABLE vehicles
(
    id         VARCHAR(36) PRIMARY KEY,
    status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',

    driver_id  VARCHAR(36)  NOT NULL,
    model      VARCHAR(100) NOT NULL,
    plate      VARCHAR(20)  NOT NULL,
    capacity   INTEGER      NOT NULL,
    color      VARCHAR(50),

    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255),

    CONSTRAINT fk_vehicle_driver FOREIGN KEY (driver_id) REFERENCES drivers (id),
    CONSTRAINT uk_owner_plate UNIQUE (driver_id, plate);
);

CREATE INDEX idx_driver_app_user ON driver_applications (user_id);
CREATE INDEX idx_vehicles_driver ON vehicles (driver_id);