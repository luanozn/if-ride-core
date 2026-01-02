package com.ifride.core.shared.config;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ErrorResponse {
    private String message;
    private String details;
    private int statusCode;
}
