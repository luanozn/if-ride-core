package com.ifride.core.events.models;

import com.ifride.core.auth.model.entity.User;

public record UserRegisteredEvent(User user) {
}
