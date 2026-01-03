package com.ifride.core.driver.model.dto;

import com.ifride.core.auth.model.dto.UserDto;
import com.ifride.core.driver.model.entity.DriverApplication;
import com.ifride.core.driver.model.enums.CnhCategory;
import com.ifride.core.driver.model.enums.DriverApplicationStatus;

import java.time.LocalDate;

public record DriverApplicationSummaryDTO(UserDto requester, DriverApplicationStatus applicationStatus,
                                          String cnhNumber, CnhCategory cnhCategory, LocalDate cnhExpiration,
                                          UserDto reviewedBy, String rejectionReason) {

    public static DriverApplicationSummaryDTO fromEntity(DriverApplication application) {
        return new DriverApplicationSummaryDTO(
                UserDto.fromEntity(application.getRequester()),
                application.getApplicationStatus(),
                application.getCnhNumber(),
                application.getCnhCategory(),
                application.getCnhExpiration(),
                UserDto.fromEntity(application.getReviewedBy()),
                application.getRejectionReason()
        );
    }
}
