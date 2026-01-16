package com.ifride.core.ride.service.validators;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.ride.model.Ride;
import com.ifride.core.ride.model.RideParticipant;
import com.ifride.core.ride.model.enums.ParticipantStatus;
import com.ifride.core.ride.model.enums.RideStatus;
import com.ifride.core.ride.repository.RideParticipantRepository;
import com.ifride.core.shared.exceptions.api.BadRequestException;
import com.ifride.core.shared.exceptions.api.ConflictException;
import com.ifride.core.shared.exceptions.api.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RideParticipantValidator {

    private final RideParticipantRepository rideParticipantRepository;

    public void validateRequest(User author, Ride ride, String pickupPoint) {
        checkActiveParticipant(ride, author);
        checkRideAvailability(ride);
        checkTimeConflict(author, ride.getDepartureTime());
        checkOwnership(author, ride);
        checkPickupPointInRide(ride, pickupPoint);
    }

    public void validateAcceptance(RideParticipant participant, String driverId) {
        checkRiderValidating(participant, driverId);
        checkRequestNotProcessed(participant);
    }

    public void validateRejection(RideParticipant participant, String driverId) {
        checkRiderValidating(participant, driverId);
        checkRequestNotProcessed(participant);
    }

    public void validateCancelling(RideParticipant participant, String passengerId) {
        checkPassengerCancelling(participant, passengerId);
        checkDepartureTime(participant);
    }

    private void checkActiveParticipant(Ride ride, User author) {
        boolean alreadyRequested = rideParticipantRepository.existsByRideIdAndPassengerIdAndParticipantStatusIn(
                ride.getId(),
                author.getId(),
                List.of(ParticipantStatus.PENDING, ParticipantStatus.ACCEPTED)
        );

        if (alreadyRequested) {
            throw new ConflictException("Você já possui uma solicitação ativa para esta carona.");
        }
    }

    private void checkRideAvailability(Ride ride) {
        if (ride.getRideStatus() != RideStatus.SCHEDULED || ride.getAvailableSeats() <= 0) {
            throw new ConflictException("Esta carona não está mais aceitando novas solicitações.");
        }
    }

    private void checkTimeConflict(User user, LocalDateTime departure) {
        var start = departure.minusHours(1);
        var end = departure.plusHours(1);
        if (rideParticipantRepository.hasConflict(user, start, end)) {
            throw new ConflictException("Conflito de horário detectado.");
        }
    }

    private void checkOwnership(User author, Ride ride) {
        if (ride.getDriver().getId().equals(author.getId())) {
            throw new ForbiddenException("Motorista não pode ser passageiro da própria carona.");
        }
    }

    private void checkPickupPointInRide(Ride ride, String pickupPoint) {
        boolean pointExists = ride.getPickupPoints().contains(pickupPoint);

        if (!pointExists) {
            throw new BadRequestException("O ponto de embarque '" + pickupPoint + "' não faz parte desta carona.");
        }
    }

    private void checkRiderValidating(RideParticipant participant, String driverId) {
        if (!participant.getRide().getDriver().getId().equals(driverId)) {
            throw new ForbiddenException("Apenas o motorista desta carona pode aceitar ou negar passageiros.");
        }
    }

    private void checkRequestNotProcessed(RideParticipant participant) {
        if (participant.getParticipantStatus() != ParticipantStatus.PENDING) {
            throw new ConflictException("Esta solicitação já foi processada, e está com o status " + participant.getParticipantStatus().name());
        }
    }

    private void checkPassengerCancelling(RideParticipant participant, String passengerId) {
        if (!participant.getPassenger().getId().equals(passengerId)) {
            throw new ForbiddenException("Você só pode cancelar a sua própria participação.");
        }
    }

    private void checkDepartureTime(RideParticipant participant) {
        if (participant.getRide().getDepartureTime().isBefore(LocalDateTime.now())) {
            throw new ConflictException("Não é possível cancelar uma participação após o horário de partida.");
        }
    }
}
