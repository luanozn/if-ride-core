package com.ifride.core.auth.model.dto;

public record LoginResponseDTO(String token, String login, Long expireDate) {
}
