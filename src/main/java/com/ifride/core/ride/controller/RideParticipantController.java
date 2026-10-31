package com.ifride.core.ride.controller;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.ride.model.dto.RideParticipantResponseDTO;
import com.ifride.core.ride.service.RideParticipantService;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/ride-participant")
@RequiredArgsConstructor
@Tag(name = "Participação", description = "Gerenciamento do status de passageiros em caronas")
@SecurityRequirement(name = "bearerAuth")
public class RideParticipantController {

    private final RideParticipantService rideParticipantService;

    @Operation(
            summary = "Busca as participações do passageiro logado",
            description = "Retorna todas as solicitações de vaga feitas pelo próprio passageiro, ordenadas pela data de requisição mais recente.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada retornada."),
            @ApiResponse(responseCode = "401", description = "Não autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão de passageiro.")
    })
    @GetMapping("/me")
    @PreAuthorize("hasRole('PASSENGER')")
    public Page<RideParticipantResponseDTO> getMyParticipations(
            @AuthenticationPrincipal User author,
            @ParameterObject @PageableDefault(sort = "requestedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return rideParticipantService.findMyParticipations(author, pageable);
    }

    @Operation(
            summary = "Aceitar passageiro",
            description = """
                    Confirma a vaga do passageiro na carona. Ação exclusiva do motorista.
                    
                    **Regras de Negócio (RN):**
                    * A solicitação deve estar no status `PENDING`.
                    * A carona deve ter vagas disponíveis.
                    
                    **Efeitos Colaterais (Visíveis para o usuário):**
                    * A vaga é reservada. Se a carona lotar, seu status muda para `FULL`.
                    * Outras solicitações do mesmo passageiro para caronas no mesmo horário são automaticamente canceladas para evitar conflito de agenda.
                    * Uma sala de chat entre motorista e passageiro é criada.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Passageiro aceito com sucesso."),
            @ApiResponse(responseCode = "401", description = "Não autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuário não é o motorista desta carona."),
            @ApiResponse(responseCode = "404", description = "Solicitação de participação não encontrada."),
            @ApiResponse(responseCode = "409", description = "Solicitação já processada ou carona sem vagas.")
    })
    @PatchMapping("/{id}/accept")
    @PreAuthorize("hasRole('DRIVER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void accept(@AuthenticationPrincipal User author, @PathVariable String id) {
        rideParticipantService.acceptParticipation(id, author.getId());
    }

    @Operation(
            summary = "Rejeitar passageiro",
            description = """
                    Nega a vaga do passageiro na carona. Ação exclusiva do motorista.
                    
                    **Regras de Negócio (RN):**
                    * A solicitação deve estar no status `PENDING`.
                    
                    **Efeitos Colaterais:**
                    * O passageiro é notificado da recusa.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Passageiro rejeitado com sucesso."),
            @ApiResponse(responseCode = "401", description = "Não autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuário não é o motorista."),
            @ApiResponse(responseCode = "404", description = "Solicitação de participação não encontrada."),
            @ApiResponse(responseCode = "409", description = "Solicitação já processada.")
    })
    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('DRIVER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@AuthenticationPrincipal User author, @PathVariable String id) {
        rideParticipantService.rejectParticipation(id, author.getId());
    }

    @Operation(
            summary = "Cancelar participação",
            description = """
                    Cancela a solicitação de vaga. Ação realizada pelo próprio passageiro.
                    
                    **Regras de Negócio (RN):**
                    * Só pode ser feito antes do horário de partida da carona.
                    * Não é possível cancelar se já estiver `REJECTED` ou `CANCELLED`.
                    
                    **Efeitos Colaterais:**
                    * Se a vaga já havia sido aceita, ela é devolvida para a carona. Se a carona estava `FULL`, volta para `SCHEDULED`.
                    * O motorista é notificado do cancelamento.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Participação cancelada com sucesso."),
            @ApiResponse(responseCode = "401", description = "Não autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuário não é o passageiro dono da solicitação."),
            @ApiResponse(responseCode = "404", description = "Solicitação de participação não encontrada."),
            @ApiResponse(responseCode = "409", description = "Horário de partida já passou ou status não permite cancelamento.")
    })
    @PatchMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal User author, @PathVariable String id) {
        rideParticipantService.cancelParticipation(id, author.getId());
    }
}