package com.ifride.core.events.ride.listeners;

import com.ifride.core.chat.service.ConversationService;
import com.ifride.core.events.ride.models.RideParticipationAcceptedEvent;
import com.ifride.core.events.ride.models.RideParticipationCancelledEvent;
import com.ifride.core.events.ride.models.RideParticipationRejectedEvent;
import com.ifride.core.ride.repository.RideParticipantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RideParticipantListenerTest {

    @Mock
    private RideParticipantRepository participantRepository;

    @Mock
    private ConversationService conversationService;

    @InjectMocks
    private RideParticipantListener listener;

    private Instant departureTime;

    @BeforeEach
    void setUp() {
        departureTime = Instant.now().plus(2, ChronoUnit.HOURS);
    }

    @Nested
    @DisplayName("Processamento de Participação Aceita")
    class ParticipationAcceptedProcessing {

        @Test
        @DisplayName("Deve rejeitar solicitações sobrepostas e criar conversa no chat ao aceitar passageiro")
        void shouldRejectOverlapsAndCreateConversation() {
            var event = new RideParticipationAcceptedEvent(
                    "passenger-1", "Passenger Name", "ride-1", "driver-1", "Driver Name", departureTime
            );

            listener.handleParticipationAccepted(event);

            verify(participantRepository).rejectOverlappingRequests(
                    eq("passenger-1"),
                    eq("ride-1"),
                    eq(departureTime.minus(1, ChronoUnit.HOURS)),
                    eq(departureTime.plus(1, ChronoUnit.HOURS))
            );

            // Verify chat creation
            verify(conversationService).createIfAbsent(
                    "ride-1",
                    "driver-1",
                    "Driver Name",
                    "passenger-1",
                    "Passenger Name"
            );
        }
    }

    @Nested
    @DisplayName("Processamento de Participação Rejeitada ou Cancelada")
    class ParticipationRejectedOrCancelledProcessing {

        @Test
        @DisplayName("Deve processar a rejeição sem erros (preparado para push notifications futuras)")
        void shouldProcessRejection() {
            var event = new RideParticipationRejectedEvent("passenger-1", "ride-1");
            listener.handleParticipationRejected(event);
        }

        @Test
        @DisplayName("Deve processar o cancelamento sem erros (preparado para push notifications futuras)")
        void shouldProcessCancellation() {
            var event = new RideParticipationCancelledEvent("driver-1", "ride-1");
            listener.handleParticipationCancelled(event);
        }
    }
}