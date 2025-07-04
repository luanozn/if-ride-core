package com.ifride.core.model.auth;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

@Getter
public enum Role {
    PASSENGER("PASSENGER_ROLE", null),
    DRIVER("DRIVER_ROLE", PASSENGER),
    ADMIN("ADMIN_ROLE", DRIVER);

    private final String role;
    private final Role inheritance;

    Role(String role, Role inheritance) {
        this.role = role;
        this.inheritance = inheritance;
    }

    public Collection<? extends GrantedAuthority> getRoleAuthority() {
        return Stream.iterate(this, Objects::nonNull, Role::getInheritance)
                .map(role -> new SimpleGrantedAuthority(role.getRole()))
                .toList();
    }
}
