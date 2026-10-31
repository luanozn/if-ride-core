package com.ifride.core.ride.controller;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.ride.model.dto.RideParticipantRequestDTO;
import com.ifride.core.ride.model.dto.RideParticipantResponseDTO;
import com.ifride.core.ride.model.dto.RideRequestDTO;
import com.ifride.core.ride.model.dto.RideResponseDTO;
import com.ifride.core.ride.model.enums.ParticipantStatus;
import com.ifride.core.ride.service.RideParticipantService;
import com.ifride.core.ride.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/rides")
@RequiredArgsConstructor
@Tag(name = "Caronas", description = "Gerenciamento de ofertas, transições de estado e busca de caronas.")
@SecurityRequirement(name = "bearerAuth")
public class RideController {

    private final RideService rideService;
    private final RideParticipantService rideParticipantService;

    @Operation(
            summary = "Ofertar nova carona",
            description = """
                    Registra uma oferta de trajeto no sistema.
                    
                    **Regras de Negócio (RN):**
                    * **Propriedade:** O veículo informado deve pertencer ao motorista autenticado.
                    * **Data/Hora:** A partida deve ser no futuro.
                    * **Capacidade:** As vagas não podem exceder a capacidade do veículo.
                    * **Conflito de Agenda:** O motorista não pode ofertar uma carona em um intervalo de 60 minutos de outra carona sua.
                    * **Recorrência:** Para caronas recorrentes (`isRecurrent = true`), informar obrigatoriamente `recurrentDay` e `recurrencyDeparture`.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Oferta criada com sucesso (Status: SCHEDULED)."),
            @ApiResponse(responseCode = "400", description = "Dados inválidos (ex: campos de recorrência ausentes)."),
            @ApiResponse(responseCode = "401", description = "Não autenticado."),
            @ApiResponse(responseCode = "403", description = "Veículo de terceiros ou data de partida no passado."),
            @ApiResponse(responseCode = "409", description = "Conflito de horário com outra carona ou excesso de passageiros para o veículo.")
    })
    @PostMapping
    @PreAuthorize("hasRole('DRIVER')")
    @ResponseStatus(HttpStatus.CREATED)
    public RideResponseDTO createRide(@AuthenticationPrincipal User author, @Valid @RequestBody RideRequestDTO rideRequestDTO) {
        return rideService.createRide(author.getId(), rideRequestDTO);
    }

    @Operation(
            summary = "Iniciar uma carona previamente ofertada",
            description = """
                    Inicia uma carona, alterando seu status para `IN_PROGRESS`.
                    
                    **Regras de Negócio (RN):**
                    * **Propriedade:** Apenas o motorista criador da oferta pode iniciá-la.
                    * **Status:** A carona deve estar `SCHEDULED` ou `FULL`. Não pode já estar `IN_PROGRESS` ou `FINISHED`.
                    * **Agenda:** O motorista não pode ter outra carona em andamento simultaneamente.
                    * **Antecedência:** O início só é permitido a partir de 30 minutos antes do horário de partida planejado.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Carona iniciada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Usuário não é o dono da carona ou o status atual não permite início."),
            @ApiResponse(responseCode = "401", description = "Não autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão de motorista."),
            @ApiResponse(responseCode = "409", description = "Motorista já possui carona em andamento ou tentativa de início com mais de 30 minutos de antecedência.")
    })
    @PatchMapping("/{rideId}/start")
    @PreAuthorize("hasRole('DRIVER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void startRide(@AuthenticationPrincipal User author, @PathVariable String rideId) {
        rideService.startRide(rideId, author);
    }

    @Operation(
            summary = "Finalizar uma carona em andamento",
            description = """
                    Finaliza uma carona, alterando seu status para `FINISHED`.
                    
                    **Regras de Negócio (RN):**
                    * **Propriedade:** Apenas o motorista criador da carona pode finalizá-la.
                    * **Status:** A carona deve estar obrigatoriamente em andamento (`IN_PROGRESS`).
                    
                    **Efeitos Colaterais:**
                    * Se a carona for recorrente, uma nova oferta idêntica será criada automaticamente para a mesma data e hora na semana seguinte.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Carona finalizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Usuário não é o dono da carona ou a carona não está em andamento."),
            @ApiResponse(responseCode = "401", description = "Não autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão de motorista."),
            @ApiResponse(responseCode = "409", description = "Conflito nas validações de estado da carona.")
    })
    @PatchMapping("/{rideId}/finish")
    @PreAuthorize("hasRole('DRIVER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void finishRide(@AuthenticationPrincipal User author, @PathVariable String rideId) {
        rideService.finishRide(rideId, author);
    }

    @Operation(
            summary = "Solicita uma vaga em uma carona",
            description = """
                    Passageiros solicitam entrada em uma carona. A solicitação nasce como `PENDING` e aguarda aprovação.
                    
                    **Regras de Negócio (RN):**
                    * **Disponibilidade:** A carona deve estar `SCHEDULED` e possuir vagas livres.
                    * **Proibição:** O motorista não pode solicitar vaga na própria carona.
                    * **Unicidade:** O passageiro não pode ter outra solicitação `PENDING` ou `ACCEPTED` para a mesma carona.
                    * **Conflito de Agenda:** O passageiro não pode ter outra carona `ACCEPTED` que conflite com o horário (intervalo de 1 hora).
                    * **Ponto de Embarque:** O `pickupPoint` deve ser um dos pontos cadastrados na carona.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Solicitação enviada com sucesso (Status: PENDING)."),
            @ApiResponse(responseCode = "400", description = "Ponto de embarque inválido ou não pertencente à carona."),
            @ApiResponse(responseCode = "401", description = "Não autenticado."),
            @ApiResponse(responseCode = "403", description = "Motorista tentando pedir vaga na própria carona."),
            @ApiResponse(responseCode = "404", description = "Carona não encontrada."),
            @ApiResponse(responseCode = "409", description = "Carona lotada, solicitação já ativa ou conflito de horário do passageiro.")
    })
    @PostMapping("/{rideId}/request-seat")
    @PreAuthorize("hasRole('PASSENGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public RideParticipantResponseDTO createRideParticipant(
            @AuthenticationPrincipal User author,
            @PathVariable String rideId,
            @Valid @RequestBody RideParticipantRequestDTO dto
    ) {
        return rideParticipantService.requestSeat(author, rideId, dto);
    }

    @Operation(
            summary = "Busca participantes de uma carona",
            description = """
                    Lista os passageiros que solicitaram ou foram aceitos em uma carona.
                    
                    **Regras de Negócio (RN):**
                    * **Permissões:** Somente o motorista responsável pela carona e administradores (Role ADMIN) podem acessar.
                    * **Filtros:** Permite filtrar por status (PENDING, ACCEPTED, REJECTED, CANCELLED).
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de participantes retornada."),
            @ApiResponse(responseCode = "401", description = "Não autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuário não é o motorista da carona nem ADMIN."),
            @ApiResponse(responseCode = "404", description = "Carona não encontrada.")
    })
    @GetMapping("/{rideId}/participants")
    @PreAuthorize("hasRole('DRIVER') or hasRole('ADMIN')")
    public Page<RideParticipantResponseDTO> getRideParticipants(
            @AuthenticationPrincipal User author,
            @PathVariable String rideId,
            @RequestParam(required = false) List<ParticipantStatus> statuses,
            @PageableDefault(sort = "requestedAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return rideParticipantService.findBy(author, rideId, statuses, pageable);
    }

    @Operation(
            summary = "Desativa a recorrência de uma carona",
            description = """
                    Impede que uma carona recorrente gere uma nova oferta automaticamente ao ser finalizada.
                    
                    **Regras de Negócio (RN):**
                    * A carona precisa ser de fato recorrente (`isRecurrent = true`).
                    * A carona não pode estar `FINISHED` ou `CANCELLED`.
                    * Apenas o motorista dono da carona pode alterar essa configuração.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Recorrência desativada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Carona não é recorrente ou já foi finalizada/cancelada."),
            @ApiResponse(responseCode = "401", description = "Não autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuário não é o dono da carona.")
    })
    @PatchMapping("/{rideId}/disable-recurrence")
    @PreAuthorize("hasRole('DRIVER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disableRecurrence(@AuthenticationPrincipal User author, @PathVariable String rideId) {
        rideService.disableRecurrence(rideId, author);
    }

    @Operation(
            summary = "Busca as caronas criadas pelo motorista logado",
            description = "Retorna o histórico paginado de todas as caronas ofertadas pelo motorista autenticado.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada retornada."),
            @ApiResponse(responseCode = "401", description = "Não autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão de motorista.")
    })
    @GetMapping("/me")
    @PreAuthorize("hasRole('DRIVER')")
    public Page<RideResponseDTO> getMyRides(
            @AuthenticationPrincipal User author,
            @ParameterObject @PageableDefault(sort = "departureTime", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return rideService.findMyRidesAsDriver(author.getId(), pageable);
    }

    @Operation(
            summary = "Busca caronas disponíveis para o passageiro",
            description = """
                    Busca e filtra caronas agendadas que possuem vagas disponíveis.
                    
                    **Regras de Negócio & Filtros:**
                    * **Origem/Destino:** Busca textual (case-insensitive) nos campos de origem e destino.
                    * **Ocultar Lotadas:** Por padrão (`includeFull = false`), caronas sem vagas (`availableSeats = 0`) não são retornadas.
                    * **Data Mínima:** Filtra por `minDepartureTime`. Se não informado, usa a data/hora atual.
                    * **Status:** Retorna apenas caronas `SCHEDULED` ou `FULL` (se `includeFull=true`).
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de caronas disponíveis retornada."),
            @ApiResponse(responseCode = "401", description = "Não autenticado."),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão de passageiro.")
    })
    @GetMapping()
    @PreAuthorize("hasRole('PASSENGER')")
    public Page<RideResponseDTO> getRides(
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination,
            @RequestParam(defaultValue = "false") boolean includeFull,
            @RequestParam(required = false) Instant minDepartureTime,
            @ParameterObject @PageableDefault(sort = "departureTime", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return rideService.findAvailableRides(origin, destination, includeFull, minDepartureTime, pageable);
    }
}