package com.ifride.core.events.emails.model;

public record ChangePasswordRequestedEvent(
        String email,
        String plainToken
) {
}
