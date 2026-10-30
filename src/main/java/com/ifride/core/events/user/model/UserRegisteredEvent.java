package com.ifride.core.events.user.model;

import com.ifride.core.auth.model.entity.User;

public record UserRegisteredEvent(User user) {
}
