package com.ifride.core.events.listeners;

import com.ifride.core.chat.service.ConversationService;
import com.ifride.core.events.models.RideParticipationAcceptedEvent;
import com.ifride.core.ride.repository.RideParticipantRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
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
}
