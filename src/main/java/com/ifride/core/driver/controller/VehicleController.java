package com.ifride.core.driver.controller;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.driver.model.dto.VehicleCreationDTO;
import com.ifride.core.driver.model.dto.VehicleResponseDTO;
import com.ifride.core.driver.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/driver/{driverId}/vehicles")
@RequiredArgsConstructor
@Tag(name = "Veículos", description = "Gerenciamento de veículos dos motoristas")
@SecurityRequirement(name = "bearerAuth")
public class VehicleController {

    private final VehicleService vehicleService;

    @Operation(
            summary = "Cadastra um novo veículo para um motorista",
            description = """
                    Registra um veículo vinculado ao motorista autenticado.
                    
                    **Regras de Negócio (RN):**
                    * **Ownership:** O `driverId` do path deve ser igual ao ID do usuário autenticado (ninguém cria veículo para outro).
                    * **Unicidade por Motorista:** A placa é única **por motorista** (constraint `uk_owner_plate`). O mesmo motorista não pode ter dois veículos com a mesma placa, mas outro motorista pode ter a mesma placa (ex: carro compartilhado entre membros da mesma família).
                    * **Role:** Requer Role `DRIVER` (herdada por ADMIN via herança de roles).
                    
                    **Integridade:**
                    * O motorista informado precisa existir na tabela `drivers` (criado via aprovação de `DriverApplication`).
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Veículo cadastrado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Dados inválidos (modelo, placa ou capacidade ausentes)."),
            @ApiResponse(responseCode = "401", description = "Não autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuário não tem Role DRIVER, ou está tentando criar veículo para outro motorista."),
            @ApiResponse(responseCode = "404", description = "Motorista não encontrado na tabela drivers."),
            @ApiResponse(responseCode = "409", description = "Veículo com esta placa já cadastrado para este motorista.")
    })
    @PostMapping
    @PreAuthorize("hasRole('DRIVER')")
    public VehicleResponseDTO create(
            @AuthenticationPrincipal User author,
            @Parameter(description = "ID do motorista (UUID) - deve ser igual ao ID do usuário autenticado")
            @PathVariable String driverId,
            @RequestBody VehicleCreationDTO vehicle
    ) {
        return vehicleService.saveBy(vehicle, driverId, author);
    }

    @Operation(
            summary = "Lista os veículos de um motorista",
            description = """
                    Retorna todos os veículos ativos vinculados a um motorista.
                    
                    **Regras de Negócio (RN):**
                    * **Ownership (Soft):** O próprio motorista sempre pode ver seus veículos.
                    * **Visão ADMIN:** Um ADMIN também pode consultar os veículos de qualquer motorista (útil para auditoria).
                    * **Bloqueio:** Qualquer outro usuário recebe 403.
                    
                    **Filtro Aplicado:**
                    * Apenas veículos com `status <> 'DELETED'` são retornados.
                    * O DTO `VehicleResponseDTO` inclui um resumo do motorista (`DriverSummaryDTO`), com ID, nome e categoria da CNH.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de veículos recuperada com sucesso (pode estar vazia)."),
            @ApiResponse(responseCode = "401", description = "Não autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuário não é o dono nem ADMIN."),
            @ApiResponse(responseCode = "404", description = "Motorista não encontrado na tabela drivers.")
    })
    @GetMapping
    @PreAuthorize("hasRole('DRIVER')")
    public List<VehicleResponseDTO> findByDriverId(
            @AuthenticationPrincipal User author,
            @PathVariable String driverId
    ) {
        return vehicleService.getByOwner(driverId, author);
    }
}