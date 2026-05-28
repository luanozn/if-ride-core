package com.ifride.core.chat.model.dto;

import com.ifride.core.chat.model.entity.Conversation;

import java.time.LocalDateTime;

public record ConversationDTO(
        String id,
        String rideId,
        String driverId,
        String driverName,
        String passengerId,
        String passengerName,
        String lastMessage,
        LocalDateTime lastMessageAt,
        LocalDateTime createdAt
) {
    public static ConversationDTO fromEntity(Conversation c) {
        return new ConversationDTO(
                c.getId(),
                c.getRideId(),
                c.getDriverId(),
                c.getDriverName(),
                c.getPassengerId(),
                c.getPassengerName(),
                c.getLastMessage(),
                c.getLastMessageAt(),
                c.getCreatedAt()
        );
    }
}
