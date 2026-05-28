package com.ifride.core.events.models;

import java.time.LocalDateTime;

public record RideParticipationAcceptedEvent(
        String passengerId,
        String passengerName,
        String acceptedRideId,
        String driverId,
        String driverName,
        LocalDateTime departureTime
) {}
