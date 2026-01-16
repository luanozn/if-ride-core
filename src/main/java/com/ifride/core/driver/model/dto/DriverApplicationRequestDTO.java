package com.ifride.core.driver.model.dto;
import com.ifride.core.driver.model.enums.CnhCategory;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record DriverApplicationRequestDTO(
        @Schema(description = "ID do usuário solicitante (deve ser o ID do próprio usuário logado)", example = "550e8400-e29b-41d4-a716-446655440000")
        String requesterId,

        @Schema(description = "Número da Carteira Nacional de Habilitação", example = "12345678910")
        String cnhNumber,

        @Schema(description = "Categoria da CNH (A, B, AB, etc.)")
        CnhCategory cnhCategory,

        @Schema(description = "Data de validade da CNH", example = "2030-12-31")
        LocalDate expiration
) {
}
