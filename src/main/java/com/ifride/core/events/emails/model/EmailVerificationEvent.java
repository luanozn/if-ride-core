package com.ifride.core.events.emails.model;

public record EmailVerificationEvent(String email, String plainToken) {
}
