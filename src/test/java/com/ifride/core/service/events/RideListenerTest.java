package com.ifride.core.service.events;

import com.ifride.core.driver.model.entity.Driver;
import com.ifride.core.events.ride.listeners.RideListener;
import com.ifride.core.events.ride.models.RideFinishedEvent;
import com.ifride.core.ride.model.Ride;
import com.ifride.core.ride.model.dto.RideRequestDTO;
import com.ifride.core.ride.service.RideService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RideListenerTest {

    @Mock
    private RideService rideService;

    @InjectMocks
    private RideListener listener;

    private Ride ride;
    private RideFinishedEvent event;

    @BeforeEach
    void setUp() {
        var driver = new Driver();
        driver.setId("driver-1");

        ride = new Ride();
        ride.setId("ride-1");
        ride.setDriver(driver);
        ride.setDepartureTime(Instant.now().plus(1, ChronoUnit.WEEKS));

        event = new RideFinishedEvent("ride-1");
    }

    @Nested
    @DisplayName("Processamento de Carona Finalizada")
    class RideFinishedProcessing {

        @Test
        @DisplayName("Deve criar uma nova ocorrência se a carona for recorrente")
        void shouldCreateNewRideWhenRecurrent() {
            ride.setRecurrent(true);
            given(rideService.findById("ride-1")).willReturn(ride);

            listener.handleRideFinished(event);

            verify(rideService).createRide(eq("driver-1"), any(RideRequestDTO.class));
        }

        @Test
        @DisplayName("Não deve criar nova ocorrência se a carona não for recorrente")
        void shouldNotCreateNewRideWhenNotRecurrent() {
            ride.setRecurrent(false);
            given(rideService.findById("ride-1")).willReturn(ride);

            listener.handleRideFinished(event);

            verify(rideService, never()).createRide(any(), any());
        }
    }
}