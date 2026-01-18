package com.ifride.core.ride.model.dto;

import com.ifride.core.auth.model.dto.UserDto;
import com.ifride.core.ride.model.RideParticipant;
import com.ifride.core.ride.model.enums.ParticipantStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record RideParticipantResponseDTO(
        @Schema(description = "Dados completos da carona solicitada")
        RideResponseDTO ride,

        @Schema(description = "Dados do passageiro que solicitou a vaga")
        UserDto passenger,

        @Schema(description = "Status da solicitação (PENDING, ACCEPTED, REJECTED, CANCELLED)", example = "PENDING")
        ParticipantStatus status,

        @Schema(description = "Data e hora em que a solicitação foi feita", example = "2026-01-16T10:00:00")
        LocalDateTime requestedAt
) {

    public static RideParticipantResponseDTO from(RideParticipant rideParticipant) {
        return new RideParticipantResponseDTO(
                RideResponseDTO.fromEntity(rideParticipant.getRide()),
                UserDto.fromEntity(rideParticipant.getPassenger()),
                rideParticipant.getParticipantStatus(),
                rideParticipant.getRequestedAt()
        );
    }
}
