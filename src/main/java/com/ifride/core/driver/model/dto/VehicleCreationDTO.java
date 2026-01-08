package com.ifride.core.driver.model.dto;

import java.math.BigInteger;

public record VehicleCreationDTO(String model, String plate, String color, BigInteger capacity) {
}
