package com.ifride.core.service.ride.service;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.driver.model.entity.Driver;
import com.ifride.core.driver.model.entity.Vehicle;
import com.ifride.core.driver.model.enums.CnhCategory;
import com.ifride.core.driver.service.DriverService;
import com.ifride.core.driver.service.VehicleService;
import com.ifride.core.events.ride.models.RideFinishedEvent;
import com.ifride.core.ride.model.Ride;
import com.ifride.core.ride.model.dto.RideRequestDTO;
import com.ifride.core.ride.model.enums.RideStatus;
import com.ifride.core.ride.repository.RideRepository;
import com.ifride.core.ride.service.RideService;
import com.ifride.core.ride.service.validators.RideValidator;
import com.ifride.core.shared.exceptions.api.ConflictException;
import com.ifride.core.shared.utils.DateTimeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RideServiceTest {

    @Mock private RideRepository repository;
    @Mock private DriverService driverService;
    @Mock private VehicleService vehicleService;
    @Mock private RideValidator validator;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private DateTimeUtils dateTimeUtils;

    @InjectMocks
    private RideService service;

    private Ride ride;
    private User driverUser;
    private Driver driver;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        driverUser = new User();
        driverUser.setId("d1");

        driver = new Driver();
        driver.setId("d1");
        driver.setUser(driverUser);
        driver.setCnhCategory(CnhCategory.AB);

        vehicle = new Vehicle();
        vehicle.setId("v1");
        vehicle.setOwner(driver);

        ride = new Ride();
        ride.setId("r1");
        ride.setDriver(driver);
        ride.setVehicle(vehicle);
        ride.setRideStatus(RideStatus.SCHEDULED);
    }

    @Nested
    @DisplayName("Criação de Carona")
    class RideCreation {

        @Test
        @DisplayName("Deve criar a carona com sucesso quando os dados forem válidos")
        void shouldCreateRideSuccessfully() {
            var dto = new RideRequestDTO(
                    "v1", "A", "B", null, Instant.now().plus(2, ChronoUnit.HOURS),
                    3, BigDecimal.TEN, false, null, null
            );

            given(driverService.findById("d1")).willReturn(driver);
            given(vehicleService.findById("v1")).willReturn(vehicle);
            given(repository.save(any(Ride.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(dateTimeUtils.getDepartureTime(eq(dto))).willReturn(Instant.now().plus(2, ChronoUnit.HOURS));

            var response = service.createRide("d1", dto);

            assertThat(response).isNotNull();
            assertThat(response.origin()).isEqualTo("A");
            assertThat(response.destination()).isEqualTo("B");

            verify(validator).validateRideCreation(driver, vehicle, dto);
            verify(repository).save(any(Ride.class));
        }
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
        @DisplayName("Deve mudar o status para IN_PROGRESS ao iniciar a carona")
        void shouldStartRide() {
            given(repository.findById("r1")).willReturn(Optional.of(ride));

            service.startRide("r1", driverUser);

            verify(validator).validateRideUpdate(ride, driverUser, RideStatus.IN_PROGRESS);
            verify(repository).updateStatus("r1", RideStatus.IN_PROGRESS);
            verifyNoInteractions(eventPublisher);
        }

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

    @Nested
    @DisplayName("Desativação de Recorrência")
    class RecurrenceManagement {

        @Test
        @DisplayName("Deve desativar a recorrência e salvar a carona")
        void shouldDisableRecurrence() {
            ride.setRecurrent(true);
            given(repository.findById("r1")).willReturn(Optional.of(ride));

            service.disableRecurrence("r1", driverUser);

            verify(validator).validateDisableRecurrence(ride, driverUser);
            assertThat(ride.isRecurrent()).isFalse();
            verify(repository).save(ride);
        }
    }
}