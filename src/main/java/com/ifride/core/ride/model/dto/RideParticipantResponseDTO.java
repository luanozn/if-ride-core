package com.ifride.core.ride.model.dto;

import com.ifride.core.auth.model.dto.UserDto;
import com.ifride.core.ride.model.RideParticipant;
import com.ifride.core.ride.model.enums.ParticipantStatus;
import java.time.LocalDateTime;

public record RideParticipantResponseDTO(RideResponseDTO ride, UserDto passenger, ParticipantStatus status, LocalDateTime requestedAt) {

    public static RideParticipantResponseDTO from(RideParticipant rideParticipant) {
        return new RideParticipantResponseDTO(
                RideResponseDTO.fromEntity(rideParticipant.getRide()),
                UserDto.fromEntity(rideParticipant.getPassenger()),
                rideParticipant.getStatus(),
                rideParticipant.getRequestedAt()
        );
    }
}
