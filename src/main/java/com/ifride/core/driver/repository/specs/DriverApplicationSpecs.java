package com.ifride.core.driver.repository.specs;

import com.ifride.core.driver.model.entity.DriverApplication;
import com.ifride.core.driver.model.enums.DriverApplicationStatus;
import jakarta.persistence.criteria.JoinType;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class DriverApplicationSpecs {

    public static Specification<DriverApplication> buildSearchSpec(
            List<DriverApplicationStatus> statuses, String email, String name) {

        var spec = Specification.where(fetchRequester());

        if (statuses != null && !statuses.isEmpty()) {
            spec = spec.and(DriverApplicationSpecs.hasStatusIn(statuses));
        }

        var emailOrName = buildEmailOrNameSpec(email, name);
        if (emailOrName != null) {
            spec = spec.and(emailOrName);
        }

        return spec;
    }

    private static Specification<DriverApplication> buildEmailOrNameSpec(String email, String name) {
        boolean hasEmail = email != null && !email.isBlank();
        boolean hasName = name != null && !name.isBlank();

        if (hasEmail && hasName) {
            return DriverApplicationSpecs.emailContains(email)
                    .or(DriverApplicationSpecs.nameContains(name));
        }
        if (hasEmail) return DriverApplicationSpecs.emailContains(email);
        if (hasName) return DriverApplicationSpecs.nameContains(name);
        return null;
    }

    private static Specification<DriverApplication> fetchRequester() {
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("requester", JoinType.LEFT);
            }
            return cb.conjunction();
        };
    }

    private static Specification<DriverApplication> hasStatusIn(List<DriverApplicationStatus> statuses) {
        return (root, query, cb) -> root.get("applicationStatus").in(statuses);
    }

    private static Specification<DriverApplication> emailContains(String email) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("requester").get("email")),
                        "%" + email.toLowerCase() + "%");
    }

    private static Specification<DriverApplication> nameContains(String name) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("requester").get("name")),
                        "%" + name.toLowerCase() + "%");
    }
}