package com.ifride.core.auth.model.entity;

import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.shared.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

@Table(name="users")
@Entity(name = "users")
@EqualsAndHashCode(callSuper = true)
@Data
public class User extends BaseEntity implements UserDetails {

    private String name;
    private String email;
    private String password;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return role.getRoleAuthority();
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }

    public boolean has(Role role) {
        return Stream.iterate(this.role, Objects::nonNull, Role::getInheritance)
                .anyMatch(userRole -> userRole.equals(role));
    }
}
