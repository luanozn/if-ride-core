package com.ifride.core.driver.controller;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.service.UserService;
import com.ifride.core.driver.model.dto.DriverApplicationRejectionDTO;
import com.ifride.core.driver.model.dto.DriverApplicationRequestDTO;
import com.ifride.core.driver.model.dto.DriverApplicationSummaryDTO;
import com.ifride.core.driver.service.DriverApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/driver-requests")
@RequiredArgsConstructor
@Log4j2
public class DriverApplicationController {

    private final UserService userService;
    private final DriverApplicationService driverApplicationService;

    @PostMapping
    @PreAuthorize("hasRole('PASSENGER')")
    public DriverApplicationSummaryDTO createDriverRequest(@AuthenticationPrincipal User author, @RequestBody DriverApplicationRequestDTO driverRequest) {
        var user = userService.findById(driverRequest.requesterId());

        log.info("O usuário {} iniciou a requisição de motorista para o usuário {}", author.getEmail() , user.getEmail());
        return driverApplicationService.createDriverApplication(author, user, driverRequest);
    }

    @PatchMapping("/{userId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public DriverApplicationSummaryDTO approveDriverRequest(@AuthenticationPrincipal User author, @PathVariable String userId) {
        return driverApplicationService.approveDriverApplication(author, userId);
    }

    @PatchMapping("/{userId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public DriverApplicationSummaryDTO rejectDriverRequest(@AuthenticationPrincipal User author, @PathVariable String userId, @RequestBody DriverApplicationRejectionDTO dto) {
        return driverApplicationService.rejectDriverApplication(author, userId, dto);
    }

    @DeleteMapping("/{requestId}")
    @PreAuthorize("hasRole('PASSENGER')")
    public void deleteDriverRequest(@AuthenticationPrincipal User author, @PathVariable String requestId) {
        driverApplicationService.delete(requestId, author);
    }

}
