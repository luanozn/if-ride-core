package com.ifride.core.admin.controller;

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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admins")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Log4j2
@Tag(name = "Gerência de Administradores", description = "Gerenciamento de fluxos que só podem ser realizados por ADMINS")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminUserService adminService;

    @PostMapping("/users/admin")
    @Operation(
            summary = "Inicia uma criação de administradores",
            description = "Cria um novo usuário admin no sistema" +
                    ""
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Solicitação criada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Tentativa de solicitar para outro usuário ou usuário já possui permissões"),
            @ApiResponse(responseCode = "409", description = "Já existe uma solicitação ativa (PENDING ou APPROVED)")
    })
    public RegisterResponseDTO createAdmin(@RequestBody RegisterRequestDTO registerDTO) {
        log.info("Criando novo ADMIN: {}", registerDTO.email());
        return RegisterResponseDTO.from(adminService.registerAdmin(registerDTO));
    }

    @PostMapping("/users/driver-direct")
    public RegisterResponseDTO createDriverDirectly(@RequestBody RegisterRequestDTO dto) {
        return RegisterResponseDTO.from(adminService.registerDriverDirectly(dto));
    }
}