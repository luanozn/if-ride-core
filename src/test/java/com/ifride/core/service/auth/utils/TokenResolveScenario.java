package com.ifride.core.service.auth.utils;

import com.ifride.core.auth.model.entity.ActionToken;
import java.time.Duration;
import java.util.Optional;

public record TokenResolveScenario(
        String description,
        Optional<ActionToken> existingToken,
        ResolveOutcome expectedOutcome,
        boolean shouldDelete,
        Duration expectedTtl
) {
    public enum ResolveOutcome { REUSE, DELETE_AND_CREATE, CREATE }
}