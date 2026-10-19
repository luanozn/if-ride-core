package com.ifride.core.driver.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ifride.core.auth.model.dto.UserDto;
import com.ifride.core.driver.model.enums.CnhCategory;
import com.ifride.core.driver.model.enums.DriverApplicationStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.ToString;

import java.time.LocalDate;

public record DriverApplicationSummaryDTO(
        @Schema(description = "Identificador único da aplicação")
        String id,

        @Schema(description = "Dados resumidos do solicitante")
        UserDto requester,

        @Schema(description = "Status atual da solicitação (PENDING, APPROVED, DENIED)")
        DriverApplicationStatus applicationStatus,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @ToString.Exclude
        @Schema(description = "Número da CNH")
        String cnhNumber,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @ToString.Exclude
        @Schema(description = "Categoria da CNH")
        CnhCategory cnhCategory,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @ToString.Exclude
        @Schema(description = "Data de validade da CNH")
        LocalDate cnhExpiration,

        @Schema(description = "Administrador que revisou a solicitação")
        UserDto reviewedBy,

        @Schema(description = "Motivo da rejeição (preenchido apenas se o status for DENIED)")
        String rejectionReason
) {}
