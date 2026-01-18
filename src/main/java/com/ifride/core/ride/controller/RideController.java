package com.ifride.core.ride.controller;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.ride.model.dto.RideParticipantRequestDTO;
import com.ifride.core.ride.model.dto.RideParticipantResponseDTO;
import com.ifride.core.ride.model.dto.RideRequestDTO;
import com.ifride.core.ride.model.dto.RideResponseDTO;
import com.ifride.core.ride.service.RideParticipantService;
import com.ifride.core.ride.service.RideService;
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
