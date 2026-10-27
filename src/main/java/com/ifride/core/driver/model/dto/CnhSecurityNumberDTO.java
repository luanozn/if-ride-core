package com.ifride.core.driver.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CnhSecurityNumberDTO (
        @Schema(description = "Número de segurança da CNH, usado somente para validação do administrador, e apagado do banco em sequência")
        String securityNumber
){ }
