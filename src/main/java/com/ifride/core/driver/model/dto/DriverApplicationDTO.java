package com.ifride.core.driver.model.dto;
import com.ifride.core.driver.model.enums.CnhCategory;

import java.time.LocalDate;

public record DriverApplicationDTO(String requesterId, String cnhNumber, CnhCategory cnhCategory, LocalDate expiration) {
}
