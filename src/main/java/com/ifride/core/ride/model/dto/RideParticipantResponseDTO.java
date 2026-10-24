package com.ifride.core.ride.model.dto;

import com.ifride.core.auth.model.dto.UserDto;
import com.ifride.core.ride.model.enums.ParticipantStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record RideParticipantResponseDTO(
        @Schema(description = "ID da participação")
        String id,

        @Schema(description = "Dados completos da carona solicitada")
        RideResponseDTO ride,

        @Schema(description = "Dados do passageiro que solicitou a vaga")
        UserDto passenger,

        @Schema(description = "Status da solicitação (PENDING, ACCEPTED, REJECTED, CANCELLED)", example = "PENDING")
        ParticipantStatus status,

        @Schema(description = "Data e hora em que a solicitação foi feita", example = "2026-01-16T10:00:00")
        Instant requestedAt
) { }
