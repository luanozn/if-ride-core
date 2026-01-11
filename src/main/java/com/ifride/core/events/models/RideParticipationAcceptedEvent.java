package com.ifride.core.events.models;

import java.time.LocalDateTime;

public record RideParticipationAcceptedEvent(
        String passengerId,
        String acceptedRideId,
        LocalDateTime departureTime
) {}