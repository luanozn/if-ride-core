package com.ifride.core.auth.model;

public record RegisterRequestDTO(String email, String password, String firstName, String lastName) {
}
