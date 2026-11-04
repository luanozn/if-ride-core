package com.ifride.core.chat.model.dto;

import java.time.Instant;

public record ConversationDTO(
        String id,
        String rideId,
        String driverId,
        String driverName,
        String passengerId,
        String passengerName,
        String lastMessage,
        Instant lastMessageAt,
        Instant createdAt
) { }
