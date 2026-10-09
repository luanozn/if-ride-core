package com.ifride.core.auth.repository.specs;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecs {

    public static Specification<User> buildSearchSpec(String searchTerm, Role role) {
        Specification<User> spec = Specification.where(null);

        if(searchTerm != null && !searchTerm.isBlank()) {
                spec = Specification.where(nameOrEmailSearch(searchTerm));
        }

        if(role != null) {
            spec = spec.and(roleSearch(role));
        }

        return spec;
    }

    private static Specification<User> nameOrEmailSearch(String searchTerm) {
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), "%" + searchTerm.toLowerCase() + "%"),
                cb.like(cb.lower(root.get("email")), "%" + searchTerm.toLowerCase() + "%")
        );
    }

    private static Specification<User> roleSearch(Role role) {
        return (root, query, cb) -> cb.equal(root.get("role"), role);
    }
}
