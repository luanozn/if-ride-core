package com.ifride.core.model.auth;

public record RegisterRequestDTO(String email, String password, String firstName, String lastName) {
}
