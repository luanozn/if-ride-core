package com.ifride.core.events.listeners;

import com.ifride.core.chat.service.ConversationService;
import com.ifride.core.events.models.RideParticipationAcceptedEvent;
import com.ifride.core.events.models.RideParticipationCancelledEvent;
import com.ifride.core.events.models.RideParticipationRejectedEvent;
import com.ifride.core.ride.repository.RideParticipantRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Log4j2
public class RideParticipantListener {

    private final RideParticipantRepository participantRepository;
    private final ConversationService conversationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleParticipationAccepted(RideParticipationAcceptedEvent event) {
        LocalDateTime startTime = event.departureTime().minusHours(1);
        LocalDateTime endTime = event.departureTime().plusHours(1);

        participantRepository.rejectOverlappingRequests(
                event.passengerId(),
                event.acceptedRideId(),
                startTime,
                endTime
        );

        conversationService.createIfAbsent(
                event.acceptedRideId(),
                event.driverId(),
                event.driverName(),
                event.passengerId(),
                event.passengerName()
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleParticipationRejected(RideParticipationRejectedEvent event) {
        log.info("Solicitação do passageiro {} na carona {} foi rejeitada.", event.passengerId(), event.rideId());
        // Ponto de extensão: enviar push notification ao passageiro quando disponível
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleParticipationCancelled(RideParticipationCancelledEvent event) {
        log.info("Passageiro cancelou participação na carona {}. Motorista {} deve ser notificado.", event.rideId(), event.driverId());
        // Ponto de extensão: enviar push notification ao motorista quando disponível
    }
}
