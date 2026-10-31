package com.ifride.core.ride.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RideParticipantRequestDTO(
        @Schema(description = "Ponto de encontro escolhido pelo passageiro (deve ser um dos pickupPoints da carona)", example = "Biblioteca")
        @NotBlank(message = "O ponto de embarque é obrigatório")
        String pickupPoint
) {}