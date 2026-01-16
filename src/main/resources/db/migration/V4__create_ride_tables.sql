CREATE TABLE rides
(
    id              VARCHAR(36) PRIMARY KEY,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    driver_id       VARCHAR(36)    NOT NULL,
    vehicle_id      VARCHAR(36)    NOT NULL,
    available_seats INT            NOT NULL,
    total_seats     INT            NOT NULL,
    origin          VARCHAR(255)   NOT NULL,
    destination     VARCHAR(255)   NOT NULL,
    departure_time  TIMESTAMP      NOT NULL,
    ride_status     VARCHAR(20)    NOT NULL DEFAULT 'SCHEDULED',
    price           DECIMAL(10, 2) NOT NULL DEFAULT 0.00,

    created_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(255),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by         VARCHAR(255),
    version            BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_ride_driver FOREIGN KEY (driver_id) REFERENCES drivers (id),
    CONSTRAINT fk_ride_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (id)
);

CREATE TABLE ride_pickup_points
(
    ride_id     VARCHAR(36)  NOT NULL,
    point_name  VARCHAR(255) NOT NULL,
    point_order INT          NOT NULL,

    PRIMARY KEY (ride_id, point_order),
    CONSTRAINT fk_pickup_ride FOREIGN KEY (ride_id) REFERENCES rides (id) ON DELETE CASCADE
);

CREATE TABLE ride_participants
(
    id                 VARCHAR(36) PRIMARY KEY,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    ride_id            VARCHAR(36) NOT NULL,
    user_id            VARCHAR(36) NOT NULL,
    participant_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pickup_point       VARCHAR(255) NOT NULL,

    created_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(255),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by         VARCHAR(255),
    version            BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_participant_ride FOREIGN KEY (ride_id) REFERENCES rides (id),
    CONSTRAINT fk_participant_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_rides_departure ON rides (departure_time);
CREATE INDEX idx_rides_driver ON rides(driver_id);
CREATE INDEX idx_rides_origin_lower ON rides (lower(origin) varchar_pattern_ops);
CREATE INDEX idx_rides_dest_lower ON rides (lower(destination) varchar_pattern_ops);

CREATE INDEX idx_participants_ride ON ride_participants (ride_id);
CREATE INDEX idx_participants_user ON ride_participants (user_id);
CREATE INDEX idx_participants_requested_at ON ride_participants (requested_at DESC);
CREATE UNIQUE INDEX idx_unique_active_participant ON ride_participants (ride_id, user_id) WHERE participant_status IN ('PENDING', 'ACCEPTED');
