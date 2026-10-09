package com.ifride.core.events.models;

import java.time.Instant;

public record RideParticipationAcceptedEvent(
        String passengerId,
        String passengerName,
        String acceptedRideId,
        String driverId,
        String driverName,
        Instant departureTime
) {}
