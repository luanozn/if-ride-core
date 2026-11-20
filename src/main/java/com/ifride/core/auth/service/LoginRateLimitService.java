package com.ifride.core.auth.service;

import com.ifride.core.auth.model.TooManyRequestsException;
import com.ifride.core.auth.model.entity.Attempt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class LoginRateLimitService {
    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    private static final int MAX_EMAIL_ATTEMPTS = 5;
    private static final int MAX_IP_ATTEMPTS = 50;
    private static final Duration ATTEMPT_WINDOW = Duration.ofMinutes(15);

    public void checkBlocked(String email, String ip) {
        checkLimit("email:" + email, MAX_EMAIL_ATTEMPTS, "Conta temporariamente bloqueada.");
        checkLimit("ip:" + ip, MAX_IP_ATTEMPTS, "Muitas tentativas deste dispositivo. Tente novamente mais tarde.");
    }

    public void loginFailed(String email, String ip) {
        registerFailure("email:" + email);
        registerFailure("ip:" + ip);
    }

    public void loginSucceeded(String email) {
        attempts.remove("email:" + email);
    }

    private void checkLimit(String key, int max, String msg) {
        var attempt = attempts.get(key);
        if (attempt != null && attempt.quantity() >= max) {
            if (attempt.lastAttempt().isAfter(Instant.now(clock).minus(ATTEMPT_WINDOW))) {
                throw new TooManyRequestsException(msg);
            } else {
                attempts.remove(key);
            }
        }
    }

    private void registerFailure(String key) {
        attempts.compute(key, (k, current) -> {
            if (current == null || current.lastAttempt().isBefore(Instant.now(clock).minus(ATTEMPT_WINDOW))) {
                return new Attempt(1, Instant.now());
            }
            return new Attempt(current.quantity() + 1, Instant.now());
        });
    }
}