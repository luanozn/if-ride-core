package com.ifride.core.service;

import com.ifride.core.ride.model.dto.RideRequestDTO;
import com.ifride.core.shared.utils.DateTimeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.threeten.extra.MutableClock;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class DateTimeUtilsTest {

    private DateTimeUtils dateTimeUtils;
    private MutableClock clock;

    private static final Instant FIXED_NOW = Instant.parse("2026-10-30T12:00:00Z");

    @BeforeEach
    void setUp() {
        clock = MutableClock.of(FIXED_NOW, ZoneOffset.UTC);
        dateTimeUtils = new DateTimeUtils(clock);
    }

    @Nested
    @DisplayName("Cálculo de Partida para Caronas")
    class DepartureCalculation {

        @Test
        @DisplayName("Deve retornar a data exata se a carona não for recorrente")
        void shouldReturnExactDateWhenNotRecurrent() {
            var exactTime = FIXED_NOW.plus(2, java.time.temporal.ChronoUnit.HOURS);
            var request = new RideRequestDTO("v1", "A", "B", null, exactTime, 3, BigDecimal.TEN, false, null, null);

            Instant result = dateTimeUtils.getDepartureTime(request);

            assertThat(result).isEqualTo(exactTime);
        }

        @Test
        @DisplayName("Deve calcular para a próxima semana se o dia for hoje mas o horário já passou")
        void shouldCalculateNextWeekIfTimePassedToday() {
            var pastTime = LocalTime.of(10, 0);

            var request = new RideRequestDTO("v1", "A", "B", null, null, 3, BigDecimal.TEN, true, DayOfWeek.FRIDAY, pastTime);

            Instant result = dateTimeUtils.getDepartureTime(request);

            LocalDate expectedDate = LocalDate.of(2026, 11, 6);
            assertThat(result.atZone(ZoneOffset.UTC).toLocalDate()).isEqualTo(expectedDate);
            assertThat(result.atZone(ZoneOffset.UTC).toLocalTime()).isEqualTo(pastTime);
        }

        @Test
        @DisplayName("Deve calcular para hoje mesmo se o dia for hoje e o horário ainda não passou")
        void shouldCalculateTodayIfTimeHasNotPassed() {
            var futureTime = LocalTime.of(18, 0);

            var request = new RideRequestDTO("v1", "A", "B", null, null, 3, BigDecimal.TEN, true, DayOfWeek.FRIDAY, futureTime);

            Instant result = dateTimeUtils.getDepartureTime(request);

            LocalDate expectedDate = LocalDate.of(2026, 10, 30);
            assertThat(result.atZone(ZoneOffset.UTC).toLocalDate()).isEqualTo(expectedDate);
            assertThat(result.atZone(ZoneOffset.UTC).toLocalTime()).isEqualTo(futureTime);
        }

        @Test
        @DisplayName("Deve calcular para a próxima Segunda-feira se a carona for no início da semana")
        void shouldCalculateNextMonday() {
            var someTime = LocalTime.of(8, 0);
            var request = new RideRequestDTO("v1", "A", "B", null, null, 3, BigDecimal.TEN, true, DayOfWeek.MONDAY, someTime);

            Instant result = dateTimeUtils.getDepartureTime(request);

            LocalDate expectedDate = LocalDate.of(2026, 11, 2); // Segunda-feira seguinte
            assertThat(result.atZone(ZoneOffset.UTC).toLocalDate()).isEqualTo(expectedDate);
        }
    }
}