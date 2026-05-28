package com.ifride.core.chat.model.dto;

public record SendMessageRequest(
        String rideId,
        String recipientId,
        String content
) {}
