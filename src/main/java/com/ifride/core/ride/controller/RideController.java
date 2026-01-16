package com.ifride.core.ride.controller;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.ride.model.dto.RideParticipantRequestDTO;
import com.ifride.core.ride.model.dto.RideParticipantResponseDTO;
import com.ifride.core.ride.model.dto.RideRequestDTO;
import com.ifride.core.ride.model.dto.RideResponseDTO;
import com.ifride.core.ride.service.RideParticipantService;
import com.ifride.core.ride.service.RideService;
import lombok.RequiredArgsConstructor;
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
public class RideController {

    private final RideService rideService;
    private final RideParticipantService rideParticipantService;

    @PostMapping
    @PreAuthorize("hasRole('DRIVER')")
    @ResponseStatus(HttpStatus.CREATED)
    public RideResponseDTO createRide(@AuthenticationPrincipal User author, @RequestBody RideRequestDTO rideRequestDTO) {
        return rideService.createRide(author.getId(), rideRequestDTO);
    }

    @PostMapping("/{rideId}/request-seat")
    @PreAuthorize("hasRole('PASSENGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public RideParticipantResponseDTO createRideParticipant(@AuthenticationPrincipal User author, @PathVariable String rideId, @RequestBody RideParticipantRequestDTO dto) {
        return rideParticipantService.requestSeat(author, rideId, dto);
    }

    @GetMapping()
    @PreAuthorize("hasRole('PASSENGER')")
    public Page<RideResponseDTO> getRides(
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination,
            @RequestParam(defaultValue = "false") boolean includeFull,
            @PageableDefault(sort = "departureTime", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return rideService.findAvailableRides(origin, destination, includeFull, pageable);
    }
}
