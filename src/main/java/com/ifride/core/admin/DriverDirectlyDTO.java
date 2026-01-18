package com.ifride.core.admin;

import com.ifride.core.auth.model.dto.RegisterRequestDTO;
import com.ifride.core.driver.model.dto.DriverApplicationRequestDTO;
import io.swagger.v3.oas.annotations.media.Schema;

public record DriverDirectlyDTO(
        @Schema(description = "Infomações do usuário que será criado")
        RegisterRequestDTO userInfo,
        @Schema(description = "Informações específicas sobre aa criação do usuário com motorista")
        DriverApplicationRequestDTO driverInformation) {
}
