package com.ifride.core.chat.model.dto;

import java.time.LocalDateTime;

public record ChatMessageDTO(
        String id,
        String rideId,
        String senderId,
        String senderName,
        String recipientId,
        String content,
        String messageStatus,
        LocalDateTime createdAt
) {}
