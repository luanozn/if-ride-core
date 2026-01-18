package com.ifride.core.driver.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record DriverApplicationRejectionDTO(
        @Schema(description = "Motivo detalhado da rejeição da solicitação", example = "Documento ilegível ou vencido.")
        String rejectionReason
) {
}
