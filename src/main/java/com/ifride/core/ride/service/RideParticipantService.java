package com.ifride.core.ride.service;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.events.models.RideParticipationAcceptedEvent;
import com.ifride.core.events.models.RideParticipationCancelledEvent;
import com.ifride.core.events.models.RideParticipationRejectedEvent;
import com.ifride.core.ride.model.RideParticipant;
import com.ifride.core.ride.model.dto.RideParticipantResponseDTO;
import com.ifride.core.ride.model.enums.ParticipantStatus;
import com.ifride.core.ride.model.enums.RideStatus;
import com.ifride.core.ride.repository.RideParticipantRepository;
import com.ifride.core.shared.exceptions.api.ConflictException;
import com.ifride.core.shared.exceptions.api.ForbiddenException;
import com.ifride.core.shared.exceptions.api.NotFoundException;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
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

        boolean alreadyRequested = repository.existsByRideIdAndPassengerIdAndParticipantStatusIn(
                rideId,
                author.getId(),
                List.of(ParticipantStatus.PENDING, ParticipantStatus.ACCEPTED)
        );

        if (alreadyRequested) {
            throw new ConflictException("Você já possui uma solicitação ativa para esta carona.");
        }

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

        if (!ride.getDriver().getId().equals(driverId)) {
            throw new ForbiddenException("Apenas o motorista desta carona pode aceitar passageiros.");
        }

        if (participant.getParticipantStatus() != ParticipantStatus.PENDING) {
            throw new ConflictException("Esta solicitação já foi processada.");
        }

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

        if (!participant.getRide().getDriver().getId().equals(driverId)) {
            throw new ForbiddenException("Apenas o motorista pode rejeitar passageiros.");
        }

        if (participant.getParticipantStatus() != ParticipantStatus.PENDING) {
            throw new ConflictException("Apenas solicitações pendentes podem ser rejeitadas.");
        }

        participant.setParticipantStatus(ParticipantStatus.REJECTED);
        repository.save(participant);

        // TODO: implementar evento para notificar o passageiro (Consistência Eventual)
        eventPublisher.publishEvent(new RideParticipationRejectedEvent(participant.getPassenger().getId(), participant.getRide().getId()));
    }

    @Transactional
    public void cancelParticipation(String participantId, String passengerId) {
        var participant = getById(participantId);

        if (!participant.getPassenger().getId().equals(passengerId)) {
            throw new ForbiddenException("Você só pode cancelar a sua própria participação.");
        }

        if (participant.getRide().getDepartureTime().isBefore(LocalDateTime.now())) {
            throw new ConflictException("Não é possível cancelar uma participação após o horário de partida.");
        }

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
