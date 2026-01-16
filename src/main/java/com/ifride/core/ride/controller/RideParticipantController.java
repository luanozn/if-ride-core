package com.ifride.core.ride.controller;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.ride.service.RideParticipantService;
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
public class RideParticipantController {

    private final RideParticipantService rideParticipantService;

    @PatchMapping("/{id}/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void accept(@AuthenticationPrincipal User author, @PathVariable String id) {
        rideParticipantService.acceptParticipation(id, author.getId());
    }

    @PatchMapping("/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@AuthenticationPrincipal User author, @PathVariable String id) {
        rideParticipantService.rejectParticipation(id, author.getId());
    }

    @PatchMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal User author, @PathVariable String id) {
        rideParticipantService.cancelParticipation(id, author.getId());
    }
}
