package com.ifride.core.shared.utils;

import com.ifride.core.ride.model.dto.RideRequestDTO;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DateTimeUtils {

    private final Clock clock;

    public Instant getDepartureTime(RideRequestDTO rideRequest) {
        return rideRequest.isRecurrent() ?
                getTimestampFromNextDayOfTheWeek(rideRequest.recurrentDay(), rideRequest.recurrencyDeparture()) :
                rideRequest.departureTime();
    }

    private Instant getTimestampFromNextDayOfTheWeek(DayOfWeek dayOfWeek, LocalTime time) {
        ZonedDateTime now = ZonedDateTime.now(clock);
        ZonedDateTime candidate = now.with(TemporalAdjusters.nextOrSame(dayOfWeek)).with(time);

        if (!candidate.toInstant().isAfter(now.toInstant())) {
            return candidate.plusWeeks(1).toInstant();
        }
        return candidate.toInstant();
    }
}