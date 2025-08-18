package com.ifride.core.auth.model.dto;

public record RegisterRequestDTO(String email, String password, String firstName, String lastName) {
}
