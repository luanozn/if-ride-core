package com.ifride.core.service.ride.service;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.driver.model.entity.Driver;
import com.ifride.core.events.ride.models.RideFinishedEvent;
import com.ifride.core.ride.model.Ride;
import com.ifride.core.ride.model.enums.RideStatus;
import com.ifride.core.ride.repository.RideRepository;
import com.ifride.core.ride.service.RideService;
import com.ifride.core.ride.service.validators.RideValidator;
import com.ifride.core.shared.exceptions.api.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RideServiceTest {

    @Mock private RideRepository repository;
    @Mock private RideValidator validator;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RideService service;

    private Ride ride;
    private User driverUser;

    @BeforeEach
    void setUp() {
        driverUser = new User();
        driverUser.setId("d1");

        var driver = new Driver();
        driver.setId("d1");
        driver.setUser(driverUser);

        ride = new Ride();
        ride.setId("r1");
        ride.setDriver(driver);
        ride.setRideStatus(RideStatus.SCHEDULED);
    }

    @Nested
    @DisplayName("Gerenciamento e Concorrência de Vagas")
    class SeatControl {

        @Test
        @DisplayName("Deve lançar exceção se a carona lotar durante a reserva (Condição de Corrida)")
        void shouldBlockSeatRaceCondition() {
            given(repository.decrementAvailableSeats("r1")).willReturn(0);

            assertThatThrownBy(() -> service.decrementAvailableSeats(ride))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Carona já está lotada");
        }

        @Test
        @DisplayName("Deve lançar exceção ao devolver vaga se o limite do veículo já foi atingido")
        void shouldBlockReturningSeatExceedingCapacity() {
            given(repository.incrementAvailableSeats("r1")).willReturn(0);

            assertThatThrownBy(() -> service.incrementAvailableSeats(ride))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Limite do veículo atingido");
        }
    }

    @Nested
    @DisplayName("Transições de Estado da Carona")
    class StateTransitions {

        @Test
        @DisplayName("Deve mudar o status e disparar evento assíncrono ao finalizar a carona")
        void shouldFinishRideAndPublishEvent() {
            ride.setRideStatus(RideStatus.IN_PROGRESS);
            given(repository.findById("r1")).willReturn(Optional.of(ride));

            service.finishRide("r1", driverUser);

            verify(validator).validateRideUpdate(ride, driverUser, RideStatus.FINISHED);
            verify(repository).updateStatus("r1", RideStatus.FINISHED);

            verify(eventPublisher).publishEvent(any(RideFinishedEvent.class));
        }
    }
}