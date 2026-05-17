package com.ifride.core.shared.utils;

import com.ifride.core.ride.model.dto.RideRequestDTO;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

public class DateTimeUtils {

    public static LocalDateTime getDepartureTime(RideRequestDTO rideRequest) {
        return rideRequest.isRecurrent() ?
                getTimestampFromNextDayOfTheWeek(rideRequest.recurrentDay(), rideRequest.recurrencyDeparture()) :
                rideRequest.departureTime();
    }

    private static LocalDateTime getTimestampFromNextDayOfTheWeek(DayOfWeek dayOfWeek, LocalTime time) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime candidate = now.with(TemporalAdjusters.nextOrSame(dayOfWeek)).with(time);

        if (!candidate.isAfter(now)) {
            return candidate.plusWeeks(1);
        }
        return candidate;
    }
}
