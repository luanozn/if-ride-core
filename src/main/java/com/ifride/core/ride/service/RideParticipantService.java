package com.ifride.core.ride.service;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.events.models.RideParticipationAcceptedEvent;
import com.ifride.core.events.models.RideParticipationCancelledEvent;
import com.ifride.core.events.models.RideParticipationRejectedEvent;
import com.ifride.core.ride.model.RideParticipant;
import com.ifride.core.ride.model.dto.RideParticipantRequestDTO;
import com.ifride.core.ride.model.dto.RideParticipantResponseDTO;
import com.ifride.core.ride.model.enums.ParticipantStatus;
import com.ifride.core.ride.model.enums.RideStatus;
import com.ifride.core.ride.repository.RideParticipantRepository;
import com.ifride.core.ride.service.validators.RideParticipantValidator;
import com.ifride.core.shared.exceptions.api.NotFoundException;
import jakarta.transaction.Transactional;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RideParticipantService {

    private final RideParticipantRepository repository;
    private final RideService rideService;
    private final RideParticipantValidator rideValidator;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public RideParticipantResponseDTO requestSeat(User author, String rideId, RideParticipantRequestDTO dto) {
        var ride = rideService.findById(rideId);

        rideValidator.validateRequest(author, ride, dto.pickupPoint());

        var rideParticipant = new RideParticipant();
        rideParticipant.setPassenger(author);
        rideParticipant.setRide(ride);

        repository.save(rideParticipant);
        return RideParticipantResponseDTO.from(rideParticipant);
    }

    @Transactional
    public void acceptParticipation(String participantId, String driverId) {
        var participant = getById(participantId);

        var ride = participant.getRide();

        rideValidator.validateAcceptance(participant, driverId);

        rideService.decrementAvailableSeats(ride);

        participant.setParticipantStatus(ParticipantStatus.ACCEPTED);
        repository.save(participant);

        if (rideService.getCurrentAvailableSeats(ride.getId()) == 0) {
            rideService.updateStatus(ride.getId(), RideStatus.FULL);
        }

        eventPublisher.publishEvent(new RideParticipationAcceptedEvent(
                participant.getPassenger().getId(),
                ride.getId(),
                ride.getDepartureTime()
        ));
    }

    @Transactional
    public void rejectParticipation(String participantId, String driverId) {
        var participant = getById(participantId);

        rideValidator.validateRejection(participant, driverId);

        participant.setParticipantStatus(ParticipantStatus.REJECTED);
        repository.save(participant);

        // TODO: implementar evento para notificar o passageiro (Consistência Eventual)
        eventPublisher.publishEvent(new RideParticipationRejectedEvent(participant.getPassenger().getId(), participant.getRide().getId()));
    }

    @Transactional
    public void cancelParticipation(String participantId, String passengerId) {
        var participant = getById(participantId);

        rideValidator.validateCancelling(participant, passengerId);

        if (participant.getParticipantStatus() == ParticipantStatus.ACCEPTED) {
            rideService.incrementAvailableSeats(participant.getRide());

            if (participant.getRide().getRideStatus() == RideStatus.FULL) {
                rideService.updateStatus(participant.getRide().getId(), RideStatus.SCHEDULED);
            }
        }

        participant.setParticipantStatus(ParticipantStatus.CANCELLED);
        repository.save(participant);

        // TODO: implementar evento para notificar o passageiro (Consistência Eventual)
        eventPublisher.publishEvent(new RideParticipationCancelledEvent(participant.getRide().getDriver().getId(), participant.getRide().getId()));
    }

    private RideParticipant getById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Solicitação não encontrada."));
    }
}
