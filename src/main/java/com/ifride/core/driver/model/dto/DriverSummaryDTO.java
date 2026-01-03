package com.ifride.core.driver.model.dto;

import com.ifride.core.driver.model.entity.Driver;

public record DriverSummaryDTO(
        String id,
        String name,
        String cnhCategory
) {
    public static DriverSummaryDTO fromEntity(Driver driver) {
        return new DriverSummaryDTO(
                driver.getId(),
                driver.getUser().getName(),
                driver.getCnhCategory().name()
        );
    }
}