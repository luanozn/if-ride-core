package com.ifride.core.events.listeners;

import com.ifride.core.events.models.RideFinishedEvent;
import com.ifride.core.ride.model.dto.RideRequestDTO;
import com.ifride.core.ride.service.RideService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Log4j2
public class RideListener {

    private final RideService rideService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRideFinished(RideFinishedEvent event) {
        log.info("Ride finished: {}", event.rideId());
        var ride = rideService.findById(event.rideId());

        if(ride.isRecurrent()) {
            log.info("Creating new recurrency for ride: {}", event.rideId());
            var request = RideRequestDTO.fromRideForRecurrency(ride);
            rideService.createRide(ride.getDriver().getId(), request);
        }
    }
}
