package com.ifride.core.driver.controller;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.service.UserService;
import com.ifride.core.driver.model.dto.DriverApplicationRejectionDTO;
import com.ifride.core.driver.model.dto.DriverApplicationRequestDTO;
import com.ifride.core.driver.model.dto.DriverApplicationSummaryDTO;
import com.ifride.core.driver.service.DriverApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/driver-requests")
@RequiredArgsConstructor
@Log4j2
@Tag(name = "Solicitações de Upgrade à Motorista", description = "Gerenciamento do fluxo de upgrade de conta para motorista")
@SecurityRequirement(name = "bearerAuth")
public class DriverApplicationController {

    private final UserService userService;
    private final DriverApplicationService driverApplicationService;

    @Operation(
            summary = "Inicia um pedido para tornar-se motorista",
            description = "Valida se o usuário já não é motorista e se não possui pedidos pendentes ou aprovados ativos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Solicitação criada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Tentativa de solicitar para outro usuário ou usuário já possui permissões"),
            @ApiResponse(responseCode = "409", description = "Já existe uma solicitação ativa (PENDING ou APPROVED)")
    })
    @PostMapping
    @PreAuthorize("hasRole('PASSENGER')")
    public DriverApplicationSummaryDTO createDriverRequest(@AuthenticationPrincipal User author, @RequestBody DriverApplicationRequestDTO driverRequest) {
        var user = userService.findById(driverRequest.requesterId());

        log.info("O usuário {} iniciou a requisição de motorista para o usuário {}", author.getEmail() , user.getEmail());
        return driverApplicationService.createDriverApplication(author, user, driverRequest);
    }

    @Operation(
            summary = "Aprova uma solicitação (Apenas ADMIN)",
            description = "Altera o status para APPROVED e dispara o evento de concessão de Role DRIVER."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitação aprovada"),
            @ApiResponse(responseCode = "404", description = "Nenhuma solicitação encontrada para o usuário"),
            @ApiResponse(responseCode = "409", description = "A solicitação não está mais em estado PENDING")
    })
    @PatchMapping("/{userId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public DriverApplicationSummaryDTO approveDriverRequest(@AuthenticationPrincipal User author, @PathVariable String userId) {
        return driverApplicationService.approveDriverApplication(author, userId);
    }

    @Operation(
            summary = "Rejeita uma solicitação (Apenas ADMIN)",
            description = "Altera o status para DENIED e registra o motivo da rejeição."
    )
    @PatchMapping("/{userId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public DriverApplicationSummaryDTO rejectDriverRequest(@AuthenticationPrincipal User author, @PathVariable String userId, @RequestBody DriverApplicationRejectionDTO dto) {
        return driverApplicationService.rejectDriverApplication(author, userId, dto);
    }

    @Operation(
            summary = "Exclui uma solicitação",
            description = "Permite que o próprio passageiro remova sua solicitação."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Solicitação excluída"),
            @ApiResponse(responseCode = "403", description = "Tentativa de excluir solicitação de terceiros")
    })
    @DeleteMapping("/{requestId}")
    @PreAuthorize("hasRole('PASSENGER')")
    public void deleteDriverRequest(@AuthenticationPrincipal User author, @PathVariable String requestId) {
        driverApplicationService.delete(requestId, author);
    }

}
