package com.ifride.core.chat.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank(message = "O ID da carona é obrigatório") String rideId,
        String recipientId,
        @NotBlank(message = "A mensagem não pode estar vazia") @Size(max = 4000) String content
) {}