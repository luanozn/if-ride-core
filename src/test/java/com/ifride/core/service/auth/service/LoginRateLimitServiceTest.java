package com.ifride.core.service.auth.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ifride.core.auth.model.TooManyRequestsException;
import com.ifride.core.auth.service.LoginRateLimitService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LoginRateLimitService")
class LoginRateLimitServiceTest {

    private MutableClock clock;
    private LoginRateLimitService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.now());
        service = new LoginRateLimitService(clock);
    }

    @Test
    @DisplayName("Bloqueia o e-mail após cinco falhas, sem depender de outro IP")
    void blocksEmailAfterFiveFailures() {
        for (int i = 0; i < 5; i++) {
            service.loginFailed("pessoa@example.com", "192.0.2." + i);
        }

        TooManyRequestsException exception = assertThrows(
                TooManyRequestsException.class,
                () -> service.checkBlocked("pessoa@example.com", "192.0.2.99"));

        assertEquals(429, exception.getStatus().value());
    }

    @Test
    @DisplayName("Bloqueia o IP após cinquenta falhas de contas diferentes")
    void blocksIpAfterFiftyFailures() {
        for (int i = 0; i < 50; i++) {
            service.loginFailed("pessoa" + i + "@example.com", "192.0.2.1");
        }

        assertThrows(TooManyRequestsException.class,
                () -> service.checkBlocked("outra@example.com", "192.0.2.1"));
    }

    @Test
    @DisplayName("Uma autenticação bem-sucedida limpa o contador do e-mail")
    void successfulLoginResetsEmailCounter() {
        for (int i = 0; i < 5; i++) {
            service.loginFailed("pessoa@example.com", "192.0.2." + i);
        }

        service.loginSucceeded("pessoa@example.com");

        assertDoesNotThrow(() -> service.checkBlocked("pessoa@example.com", "192.0.2.99"));
    }

    @Test
    @DisplayName("Libera e-mail e IP depois de expirados os quinze minutos")
    void expiresFailuresAfterWindow() {
        for (int i = 0; i < 5; i++) {
            service.loginFailed("pessoa@example.com", "192.0.2.1");
        }
        clock.advance(Duration.ofMinutes(16));

        assertDoesNotThrow(() -> service.checkBlocked("pessoa@example.com", "192.0.2.1"));
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> current;

        private MutableClock(Instant initial) {
            this.current = new AtomicReference<>(initial);
        }

        void advance(Duration duration) {
            current.updateAndGet(instant -> instant.plus(duration));
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current.get();
        }
    }
}
