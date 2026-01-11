package com.ifride.core.ride.service;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.events.models.RideParticipationAcceptedEvent;
import com.ifride.core.ride.model.RideParticipant;
import com.ifride.core.ride.model.dto.RideParticipantResponseDTO;
import com.ifride.core.ride.model.enums.ParticipantStatus;
import com.ifride.core.ride.model.enums.RideStatus;
import com.ifride.core.ride.repository.RideParticipantRepository;
import com.ifride.core.shared.exceptions.api.ConflictException;
import com.ifride.core.shared.exceptions.api.ForbiddenException;
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
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public RideParticipantResponseDTO requestSeat(User author, String rideId) {
        var ride = rideService.findById(rideId);

        var start = ride.getDepartureTime().minusHours(1);
        var end = ride.getDepartureTime().plusHours(1);

        if (ride.getRideStatus() != RideStatus.SCHEDULED || ride.getAvailableSeats() <= 0) {
            throw new ConflictException("Esta carona não está mais aceitando novas solicitações.");
        }

        if (ride.getDriver().getId().equals(author.getId())) {
            throw new ForbiddenException("Você não pode solicitar vaga na sua própria carona.");
        }

        if (repository.hasConflict(author, start, end)) {
            throw new ConflictException("Você já possui uma carona aceita em um horário conflitante.");
        }

        var rideParticipant = new RideParticipant();
        rideParticipant.setPassenger(author);
        rideParticipant.setRide(ride);

        repository.save(rideParticipant);
        return RideParticipantResponseDTO.from(rideParticipant);
    }

    @Transactional
    public void acceptParticipation(String participantId, String driverId) {
        var participant = repository.findById(participantId)
                .orElseThrow(() -> new NotFoundException("Solicitação não encontrada."));

        var ride = participant.getRide();

        if (!ride.getDriver().getId().equals(driverId)) {
            throw new ForbiddenException("Apenas o motorista desta carona pode aceitar passageiros.");
        }

        if (participant.getStatus() != ParticipantStatus.PENDING) {
            throw new ConflictException("Esta solicitação já foi processada.");
        }

        rideService.decrementAvailableSeats(ride);

        participant.setStatus(ParticipantStatus.ACCEPTED);
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
}
