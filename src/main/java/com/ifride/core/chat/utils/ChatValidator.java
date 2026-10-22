package com.ifride.core.chat.utils;

import com.ifride.core.ride.model.enums.ParticipantStatus;
import com.ifride.core.ride.service.RideParticipantService;
import com.ifride.core.ride.service.RideService;
import com.ifride.core.shared.exceptions.api.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatValidator {

    private final RideParticipantService participantService;
    private final RideService rideService;

    public void validateUserInRide(String rideId, String senderId) {
        boolean isParticipant = participantService
                .existsByRideIdAndPassengerIdAndParticipantStatus(rideId, senderId, ParticipantStatus.ACCEPTED);
        boolean isDriver = rideService.isDriverOfRide(senderId, rideId);

        if (!isParticipant && !isDriver) {
            throw new ForbiddenException("Você não tem permissão para acessar o chat desta carona.");
        }
    }
}
