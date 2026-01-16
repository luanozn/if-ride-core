package com.ifride.core.driver.controller;

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
            description = "O usuário deve ser um motorista cadastrado. Não é permitido cadastrar a mesma placa para o mesmo motorista."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Veículo cadastrado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Usuário não tem permissão de motorista"),
            @ApiResponse(responseCode = "404", description = "Motorista não encontrado"),
            @ApiResponse(responseCode = "409", description = "Veículo com esta placa já cadastrado para este motorista")
    })
    @PostMapping
    @PreAuthorize("hasRole('DRIVER')")
    public VehicleResponseDTO create(
            @Parameter(description = "ID do motorista (UUID)") @PathVariable String driverId,
            @RequestBody VehicleCreationDTO vehicle
    ) {
        return vehicleService.saveBy(vehicle, driverId);
    }


    @Operation(
            summary = "Lista todos os veículos de um motorista",
            description = "Retorna uma lista com todos os veículos vinculados ao ID do motorista fornecido."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista recuperada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Motorista não encontrado")
    })
    @GetMapping
    @PreAuthorize("hasRole('DRIVER')")
    public List<VehicleResponseDTO> findByDriverId(@PathVariable String driverId) {
        return vehicleService.getByOwner(driverId);
    }
}
