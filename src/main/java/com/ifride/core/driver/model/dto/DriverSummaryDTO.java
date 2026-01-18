package com.ifride.core.driver.model.dto;

import com.ifride.core.driver.model.entity.Driver;
import io.swagger.v3.oas.annotations.media.Schema;

public record DriverSummaryDTO(
        @Schema(description = "ID único do motorista (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
        String id,

        @Schema(description = "Nome do motorista", example = "Arthur Dent")
        String name,

        @Schema(description = "Categoria da CNH do motorista", example = "B")
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