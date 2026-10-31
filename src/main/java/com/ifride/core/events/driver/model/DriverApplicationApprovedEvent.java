package com.ifride.core.events.driver.model;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.driver.model.entity.DriverApplication;

public record DriverApplicationApprovedEvent(DriverApplication driverApplication, User author) {
}
