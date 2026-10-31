package com.ifride.core.driver.model.dto;

import com.ifride.core.driver.model.enums.CnhCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record DriverApplicationRequestDTO(
        @Schema(description = "ID do usuário solicitante (deve ser o ID do próprio usuário logado)", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotBlank(message = "O ID do solicitante é obrigatório")
        String requesterId,

        @Schema(description = "Número da Carteira Nacional de Habilitação (11 dígitos)", example = "12345678910")
        @NotBlank(message = "O número da CNH é obrigatório")
        @Pattern(regexp = "\\d{11}", message = "A CNH deve conter exatamente 11 dígitos numéricos")
        String cnhNumber,

        @Schema(description = "Categoria da CNH (A, B, AB, etc.)")
        @NotNull(message = "A categoria da CNH é obrigatória")
        CnhCategory cnhCategory,

        @Schema(description = "Número de segurança da CNH (4 dígitos)", example = "1234")
        @NotBlank(message = "O código de segurança da CNH é obrigatório")
        String cnhSecurityNumber,

        @Schema(description = "Data de validade da CNH (deve ser uma data futura)", example = "2030-12-31")
        @NotNull(message = "A validade da CNH é obrigatória")
        @Future(message = "A validade da CNH deve ser uma data futura")
        LocalDate expiration
) {}