package com.ifride.core.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank(message = "O ID da carona é obrigatório")
        @Schema(description = "ID da carona", example = "r0000000-0000-0000-0000-000000000001")
        String rideId,

        @NotBlank(message = "A mensagem deve ter um destinatário")
        @Schema(description = "ID do destinatário da mensagem", example = "t0000000-0000-0000-0000-000000000001")
        String recipientId,

        @NotBlank(message = "A mensagem não pode estar vazia")
        @Size(max = 4000)
        @Schema(description = "Conteúdo da mensagem em texto plano", example = "Olá, tudo bem? Essa mensagem foi enviada para a documentação!")
        String content
) {}