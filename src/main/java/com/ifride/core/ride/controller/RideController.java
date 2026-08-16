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
import java.util.List;
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
@Tag(name = "Caronas", description = "Gerenciamento de ofertas e solicitações de carona")
@SecurityRequirement(name = "bearerAuth")
public class RideController {

    private final RideService rideService;
    private final RideParticipantService rideParticipantService;

    @Operation(
            summary = "Ofertar nova carona",
            description = """
                        Registra uma oferta de trajeto no sistema.
                    
                        **Regras de Negócio (RN):**
                        * **Propriedade:** O veículo informado deve pertencer ao motorista logado.
                        * **Antecedência:** A partida deve ser posterior ao horário atual ($t_{partida} > t_{agora}$).
                        * **Capacidade:** O número de vagas ofertadas não pode exceder a lotação do veículo.
                        * **Intersecção:** O motorista não pode ter outra carona em um intervalo de 60 minutos.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Oferta criada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Veículo de terceiros ou data retroativa"),
            @ApiResponse(responseCode = "409", description = "Conflito de horário ou excesso de passageiros para o veículo"),
            @ApiResponse(responseCode = "409", description = "Excesso de passageiros para o veículo")
    })
    @PostMapping
    @PreAuthorize("hasRole('DRIVER')")
    @ResponseStatus(HttpStatus.CREATED)
    public RideResponseDTO createRide(@AuthenticationPrincipal User author, @RequestBody RideRequestDTO rideRequestDTO) {
        return rideService.createRide(author.getId(), rideRequestDTO);
    }

    @Operation(
            summary = "Iniciar uma carona previamente ofertada",
            description = """
                    Inicia uma carona, alterando seu status para em andamento.
                
                    **Regras de Negócio (RN):**
                    * **Propriedade:** Apenas o motorista criador da oferta pode iniciá-la.
                    * **Status da Carona:** A carona não pode estar previamente iniciada ou finalizada.
                    * **Unicidade:** O motorista não pode possuir outra carona em andamento.
                    * **Antecedência:** O início só é permitido a partir de 30 minutos antes do horário de partida planejado ($t_{agora} \\ge t_{partida} - 30\\text{ min}$).
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Carona iniciada com sucesso"),
            @ApiResponse(responseCode = "400", description = "O usuário não é o dono da carona ou a carona já está em andamento/finalizada"),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão de motorista"),
            @ApiResponse(responseCode = "409", description = "Motorista já possui carona em andamento ou tentativa de início com mais de 30 minutos de antecedência")
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
                    Finaliza uma carona, alterando seu status.
                
                    **Regras de Negócio (RN):**
                    * **Propriedade:** Apenas o motorista criador da carona pode finalizá-la.
                    * **Status da Carona:** A carona deve estar obrigatoriamente em andamento (`IN_PROGRESS`).
                    * **Recorrência (Assíncrono):** A finalização dispara um evento que cria automaticamente uma nova oferta de carona para a próxima data correspondente, caso a carona original seja recorrente.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Carona finalizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Usuário não é o dono da carona ou a carona não está em andamento"),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão de motorista"),
            @ApiResponse(responseCode = "409", description = "Conflito nas validações de estado da carona")
    })
    @PatchMapping("/{rideId}/finish")
    @PreAuthorize("hasRole('DRIVER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void finishRide(@AuthenticationPrincipal User author, @PathVariable String rideId) {
        rideService.finishRide(rideId, author);
    }

    @Operation(
            summary = "Solicita uma vaga em uma carona",
            description = "Passageiros solicitam entrada. O motorista precisará aceitar posteriormente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Solicitação enviada"),
            @ApiResponse(responseCode = "403", description = "Motorista não pode pedir vaga na própria carona"),
            @ApiResponse(responseCode = "404", description = "Carona não encontrada"),
            @ApiResponse(responseCode = "409", description = "Carona lotada OU passageiro já possui solicitação ativa OU conflito de horário do passageiro")
    })
    @PostMapping("/{rideId}/request-seat")
    @PreAuthorize("hasRole('PASSENGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public RideParticipantResponseDTO createRideParticipant(@AuthenticationPrincipal User author, @PathVariable String rideId, @RequestBody RideParticipantRequestDTO dto) {
        return rideParticipantService.requestSeat(author, rideId, dto);
    }

    @Operation(
            summary = "Busca participantes de uma carona",
            description = """
                                Lista todos os passageiros que participaram, ou solicitaram participação em uma carona.
                    
                            **Regras de Negócio (RN):**
                            * **Permissões:** Somente o motorista responsável pela carona e os administradores do sistema podem requisitar participantes.
                    """
    )
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
            description = "Impede que a carona gere uma nova ocorrência automática ao ser finalizada."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Recorrência desativada"),
            @ApiResponse(responseCode = "400", description = "Carona não é recorrente, ou já foi finalizada/cancelada"),
            @ApiResponse(responseCode = "403", description = "Usuário não é o dono da carona")
    })
    @PatchMapping("/{rideId}/disable-recurrence")
    @PreAuthorize("hasRole('DRIVER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disableRecurrence(@AuthenticationPrincipal User author, @PathVariable String rideId) {
        rideService.disableRecurrence(rideId, author);
    }

    @Operation(summary = "Busca as caronas criadas pelo motorista logado")
    @GetMapping("/me")
    @PreAuthorize("hasRole('DRIVER')")
    public Page<RideResponseDTO> getMyRides(
            @AuthenticationPrincipal User author,
            @ParameterObject @PageableDefault(sort = "departureTime", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return rideService.findMyRidesAsDriver(author.getId(), pageable);
    }

    @Operation(
            summary = "Busca caronas disponíveis",
            description = "Filtra caronas por origem e destino. Por padrão, oculta caronas lotadas."
    )
    @GetMapping()
    @PreAuthorize("hasRole('PASSENGER')")
    public Page<RideResponseDTO> getRides(
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination,
            @RequestParam(defaultValue = "false") boolean includeFull,
            @ParameterObject @PageableDefault(sort = "departureTime", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return rideService.findAvailableRides(origin, destination, includeFull, pageable);
    }
}
