package com.ifride.core.driver.repository.specs;

import com.ifride.core.driver.model.entity.DriverApplication;
import com.ifride.core.driver.model.enums.DriverApplicationStatus;
import jakarta.persistence.criteria.JoinType;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class DriverApplicationSpecs {

    public static Specification<DriverApplication> buildSearchSpec(
            List<DriverApplicationStatus> statuses, String search, String cnh) {

        var spec = Specification.where(fetchRequester());

        if (statuses != null && !statuses.isEmpty()) {
            spec = spec.and(DriverApplicationSpecs.hasStatusIn(statuses));
        }

        var emailOrName = buildBySearch(search);
        if (emailOrName != null) {
            spec = spec.and(emailOrName);
        }

        if (cnh != null && !cnh.isBlank()) {
            spec = spec.and(DriverApplicationSpecs.cnhIs(cnh));
        }

        return spec;
    }

    private static Specification<DriverApplication> buildBySearch(String search) {
        boolean hasSearch = search != null && !search.isBlank();

        if (hasSearch) {
            return DriverApplicationSpecs.emailContains(search)
                    .or(DriverApplicationSpecs.nameContains(search));
        }
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

    private static Specification<DriverApplication> cnhIs(String cnh) {
        return (root, query , cb) -> cb.equal(root.get("cnh_number"), cnh);
    }
}