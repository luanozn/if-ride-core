package com.ifride.core.shared.rate_limit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ifride.core.auth.model.TooManyRequestsException;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.TimeMeter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final Clock clock;

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(2))
            .maximumSize(100_000)
            .build();

    public void checkLimit(
            RateLimitOperation operation,
            String identifier,
            int maxAttempts,
            Duration window
    ) {
        String key = buildKey(operation, identifier);

        Bucket bucket = buckets.get(
                key,
                k -> createBucket(maxAttempts, window)
        );

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            Instant availableAt = Instant.now(clock).plusNanos(probe.getNanosToWaitForRefill());

            String formattedDate = LocalDateTime
                    .ofInstant(availableAt, ZoneId.of("America/Sao_Paulo"))
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm:ss"));

            throw new TooManyRequestsException(
                    "Muitas tentativas. Tente novamente após " + formattedDate
            );
        }
    }

    public void resetLimit(RateLimitOperation operation, String identifier) {
        buckets.invalidate(buildKey(operation, identifier));
    }

    private String buildKey(RateLimitOperation operation, String identifier) {
        String normalized = identifier == null ? "" : identifier.trim().toLowerCase(Locale.ROOT);
        return operation.name() + ":" + normalized;
    }

    private Bucket createBucket(int maxAttempts, Duration window) {
        return Bucket.builder()
                .withCustomTimePrecision(clockTimeMeter())
                .addLimit(limit -> limit
                        .capacity(maxAttempts)
                        .refillIntervally(maxAttempts, window))
                .build();
    }

    private TimeMeter clockTimeMeter() {
        return new TimeMeter() {
            @Override
            public long currentTimeNanos() {
                Instant now = Instant.now(clock);
                return TimeUnit.SECONDS.toNanos(now.getEpochSecond()) + now.getNano();
            }

            @Override
            public boolean isWallClockBased() {
                return true;
            }
        };
    }
}