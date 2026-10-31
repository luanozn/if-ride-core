package com.ifride.core.driver.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DriverApplicationRejectionDTO(
        @Schema(description = "Motivo detalhado da rejeição da solicitação", example = "Documento ilegível ou vencido.")
        @NotBlank(message = "O motivo da rejeição é obrigatório")
        @Size(max = 255, message = "O motivo da rejeição não pode exceder 255 caracteres")
        String rejectionReason
) {}