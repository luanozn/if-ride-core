package com.ifride.core.ride.controller;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.ride.service.RideParticipantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            summary = "Aceitar passageiro",
            description = """
            Ação exclusiva do motorista da carona.
        
            **Regras de Negócio:**
            * O solicitante deve ser o motorista dono da carona.
            * A solicitação deve estar no status PENDING.
            * A carona deve ter vagas disponíveis.
        
            **Efeitos Colaterais:**
            * Decrementa `availableSeats` da carona.
            * Altera status da carona para FULL se as vagas esgotarem.
            * Dispara `RideParticipationAcceptedEvent`.
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Passageiro aceito com sucesso"),
            @ApiResponse(responseCode = "403", description = "Usuário não é o motorista desta carona"),
            @ApiResponse(responseCode = "409", description = "Solicitação já processada ou carona sem vagas")
    })
    @PatchMapping("/{id}/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void accept(@AuthenticationPrincipal User author, @PathVariable String id) {
        rideParticipantService.acceptParticipation(id, author.getId());
    }

    @Operation(
            summary = "Rejeitar passageiro",
            description = """
            Ação exclusiva do motorista da carona.
        
            **Regras de Negócio:**
            * Só pode ser feito caso a requisição não tenha sido processada (Status seja igual a PENDING).
            * Só pode ser feito pelo motorista da carona.
        
            **Efeitos Colaterais:**
            * Dispara `RideParticipationRejectedEvent` (Consistência eventual para notificação).
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Passageiro rejeitado"),
            @ApiResponse(responseCode = "403", description = "Usuário não é o motorista"),
            @ApiResponse(responseCode = "409", description = "Solicitação já processada")
    })
    @PatchMapping("/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@AuthenticationPrincipal User author, @PathVariable String id) {
        rideParticipantService.rejectParticipation(id, author.getId());
    }

    @Operation(
            summary = "Cancelar participação",
            description = """
            Ação realizada pelo passageiro.
        
            **Regras de Negócio:**
            * Só pode ser feito antes do horário de partida da carona.
            * Só pode ser feito pelo próprio passageiro solicitante.
        
            **Efeitos Colaterais:**
            * Se o status era ACCEPTED, incrementa `availableSeats`.
            * Retorna status da carona para SCHEDULED se estava FULL.
            * Dispara `RideParticipationCancelledEvent`.
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Participação cancelada"),
            @ApiResponse(responseCode = "403", description = "Usuário não é o passageiro dono da solicitação"),
            @ApiResponse(responseCode = "409", description = "Horário de partida já passou")
    })
    @PatchMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal User author, @PathVariable String id) {
        rideParticipantService.cancelParticipation(id, author.getId());
    }
}
