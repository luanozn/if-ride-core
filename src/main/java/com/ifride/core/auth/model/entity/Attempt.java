package com.ifride.core.auth.model.entity;

import java.time.Instant;

public record Attempt(Integer quantity, Instant lastAttempt) {
}
