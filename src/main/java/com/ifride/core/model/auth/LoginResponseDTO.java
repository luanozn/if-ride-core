package com.ifride.core.model.auth;

public record LoginResponseDTO(String token, String login, Long expireDate) {
}
