package com.ifride.core.auth.model;

public record LoginResponseDTO(String token, String login, Long expireDate) {
}
