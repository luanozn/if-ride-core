CREATE TABLE rides
(
    id              VARCHAR(36) PRIMARY KEY,
    driver_id       VARCHAR(36)    NOT NULL,
    vehicle_id      VARCHAR(36)    NOT NULL,
    available_seats INT            NOT NULL,
    total_seats     INT            NOT NULL,
    origin          VARCHAR(255)   NOT NULL,
    destination     VARCHAR(255)   NOT NULL,
    departure_time  TIMESTAMP      NOT NULL,
    ride_status     VARCHAR(20)    NOT NULL DEFAULT 'SCHEDULED',
    price           DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    created_at      TIMESTAMP      NOT NULL,
    updated_at      TIMESTAMP,

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
    ride_id            VARCHAR(36) NOT NULL,
    user_id            VARCHAR(36) NOT NULL,
    participant_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at         TIMESTAMP   NOT NULL,
    updated_at         TIMESTAMP,

    CONSTRAINT fk_participant_ride FOREIGN KEY (ride_id) REFERENCES rides (id),
    CONSTRAINT fk_participant_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_rides_departure ON rides (departure_time);
CREATE INDEX idx_participants_ride ON ride_participants (ride_id);
CREATE INDEX idx_participants_user ON ride_participants (user_id);