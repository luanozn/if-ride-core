package com.ifride.core.driver.controller;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.service.UserService;
import com.ifride.core.driver.model.dto.DriverRequestChangeStatusDTO;
import com.ifride.core.driver.model.dto.DriverRequestDTO;
import com.ifride.core.driver.model.entity.Driver;
import com.ifride.core.driver.model.entity.DriverRequest;
import com.ifride.core.driver.service.DriverRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController("/v1/driver-request")
@RequiredArgsConstructor
@Log4j2

public class DriverRequestController {

    private final UserService userService;
    private final DriverRequestService driverRequestService;

    @PostMapping
    @PreAuthorize("hasRole('PASSENGER')")
    public DriverRequest createDriverRequest(@AuthenticationPrincipal User author, @RequestBody DriverRequestDTO driverRequest) {
        var user = userService.findById(driverRequest.requesterId());

        log.info("O usuário {} iniciou a requisição de motorista para o usuário {}", author.getEmail() , user.getEmail());
        return driverRequestService.createDriverRequest(author, user, driverRequest);
    }

    @PatchMapping("/{userId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public Driver approveDriverRequest(@AuthenticationPrincipal User author, @PathVariable String userId) {
        return driverRequestService.approveDriveRequest(author, userId);
    }

    @PatchMapping("/{userId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public DriverRequest rejectDriverRequest(@AuthenticationPrincipal User author, @PathVariable String userId, @RequestBody DriverRequestChangeStatusDTO dto) {
        return driverRequestService.rejectDriveRequest(author, userId, dto);
    }

}
