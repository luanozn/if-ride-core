package com.ifride.core.shared.utils;

import com.ifride.core.ride.model.dto.RideRequestDTO;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

public class DateTimeUtils {

    public static Instant getDepartureTime(RideRequestDTO rideRequest) {
        return rideRequest.isRecurrent() ?
                getTimestampFromNextDayOfTheWeek(rideRequest.recurrentDay(), rideRequest.recurrencyDeparture()) :
                rideRequest.departureTime();
    }

    private static Instant getTimestampFromNextDayOfTheWeek(DayOfWeek dayOfWeek, LocalTime time) {
        Instant now = Instant.now();
        Instant candidate = now.with(TemporalAdjusters.nextOrSame(dayOfWeek)).with(time);

        if (!candidate.isAfter(now)) {
            return candidate.plus(1, ChronoUnit.WEEKS);
        }
        return candidate;
    }
}
