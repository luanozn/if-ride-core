package com.ifride.core.ride.controller;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.ride.model.dto.RideRequestDTO;
import com.ifride.core.ride.model.dto.RideResponseDTO;
import com.ifride.core.ride.service.RideService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/rides")
@RequiredArgsConstructor
public class RideController {

    private final RideService rideService;

    @PostMapping
    @PreAuthorize("hasRole('DRIVER')")
    public RideResponseDTO createRide(@AuthenticationPrincipal User author, @RequestBody RideRequestDTO rideRequestDTO) {
        return rideService.createRide(author.getId(), rideRequestDTO);
    }
}
