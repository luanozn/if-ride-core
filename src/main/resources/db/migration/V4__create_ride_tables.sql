CREATE TABLE ride
(
    id              VARCHAR PRIMARY KEY,
    driver_id       VARCHAR NOT NULL,
    origin          VARCHAR(255),
    destination     VARCHAR(255),
    departure_date  TIMESTAMP,
    number_of_seats INTEGER,
    occupied_seats  INTEGER         DEFAULT 0,
    status          VARCHAR(50)     DEFAULT 'SCHEDULED',
    version         BIGINT NOT NULL DEFAULT 0,

    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ride_driver FOREIGN KEY (driver_id) REFERENCES users (id)
);

CREATE TABLE ride_pickup_points
(
    ride_id    VARCHAR      NOT NULL,
    point_name VARCHAR(255) NOT NULL,

    CONSTRAINT fk_pickup_ride FOREIGN KEY (ride_id) REFERENCES ride (id) ON DELETE CASCADE
);