package com.ifride.core.ride.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record RideParticipantRequestDTO(
        @Schema(description = "Ponto de encontro escolhido pelo passageiro (deve ser um dos pickupPoints da carona)", example = "Biblioteca")
        String pickupPoint
) {
}
