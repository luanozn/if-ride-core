package com.ifride.core.driver.model.dto;

import com.ifride.core.auth.model.dto.UserDto;
import com.ifride.core.driver.model.entity.DriverApplication;
import com.ifride.core.driver.model.enums.CnhCategory;
import com.ifride.core.driver.model.enums.DriverApplicationStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record DriverApplicationSummaryDTO(
        @Schema(description = "Dados resumidos do solicitante")
        UserDto requester,

        @Schema(description = "Status atual da solicitação (PENDING, APPROVED, DENIED)")
        DriverApplicationStatus applicationStatus,

        @Schema(description = "Número da CNH")
        String cnhNumber,

        @Schema(description = "Categoria da CNH")
        CnhCategory cnhCategory,

        @Schema(description = "Data de validade da CNH")
        LocalDate cnhExpiration,

        @Schema(description = "Administrador que revisou a solicitação")
        UserDto reviewedBy,

        @Schema(description = "Motivo da rejeição (preenchido apenas se o status for DENIED)")
        String rejectionReason
) {

    public static DriverApplicationSummaryDTO fromEntity(DriverApplication application) {
        var reviewedByDto = application.getReviewedBy() != null ? UserDto.fromEntity(application.getReviewedBy()) : null;

        return new DriverApplicationSummaryDTO(
                UserDto.fromEntity(application.getRequester()),
                application.getApplicationStatus(),
                application.getCnhNumber(),
                application.getCnhCategory(),
                application.getCnhExpiration(),
                reviewedByDto,
                application.getRejectionReason()
        );
    }
}
