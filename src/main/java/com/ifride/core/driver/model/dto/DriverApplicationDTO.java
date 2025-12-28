package com.ifride.core.driver.model.dto;
import com.ifride.core.driver.model.enums.CnhCategory;

import java.time.LocalDateTime;

public record DriverApplicationDTO(String requesterId, String cnhNumber, CnhCategory cnhCategory, LocalDateTime expiration) {
}
