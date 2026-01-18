package com.ifride.core.admin.controller;

import com.ifride.core.admin.DriverDirectlyDTO;
import com.ifride.core.admin.service.AdminUserService;
import com.ifride.core.auth.model.dto.RegisterRequestDTO;
import com.ifride.core.auth.model.dto.RegisterResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/admins")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Log4j2
@Tag(name = "Gerência de Administradores", description = "Gerenciamento de fluxos que só podem ser realizados por ADMINS")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminUserService adminService;

    @Operation(
            summary = "Inicia uma criação de administradores",
            description = "Cria um novo usuário admin no sistema"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "ADMIN criado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas ou usuário não encontrado")
    })
    @PostMapping("/users/admin")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponseDTO createAdmin(@RequestBody RegisterRequestDTO registerDTO) {
        log.info("Criando novo ADMIN: {}", registerDTO.email());
        return RegisterResponseDTO.from(adminService.registerAdmin(registerDTO));
    }

    @Operation(
            summary = "Inicia uma criação de motoristas diretamente, sem passar pelo fluxo de aplicação",
            description = "Cria um novo usuário MOTORISTA no sistema, sem processo de aplicação"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Motorista criado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas ou usuário não encontrado")
    })
    @PostMapping("/users/driver-direct")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponseDTO createDriverDirectly(@RequestBody DriverDirectlyDTO registerDTO) {
        return RegisterResponseDTO.from(adminService.registerDriverDirectly(registerDTO));
    }
}