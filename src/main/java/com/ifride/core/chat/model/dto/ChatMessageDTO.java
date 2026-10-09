package com.ifride.core.chat.model.dto;

import java.time.Instant;

public record ChatMessageDTO(
        String id,
        String rideId,
        String senderId,
        String senderName,
        String recipientId,
        String content,
        String messageStatus,
        Instant createdAt
) {}
