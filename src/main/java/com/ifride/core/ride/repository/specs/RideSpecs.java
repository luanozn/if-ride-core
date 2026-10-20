package com.ifride.core.ride.repository.specs;


import com.ifride.core.ride.model.Ride;
import java.time.Instant;
import java.util.List;

import com.ifride.core.ride.model.enums.RideStatus;
import com.ifride.core.shared.model.enums.Status;
import org.springframework.data.jpa.domain.Specification;

public class RideSpecs {

    public static Specification<Ride> buildFindAllSpecs(String origin, String destination, boolean includeFull, Instant minDepartureTime) {
        var spec = Specification.where(includeFull(includeFull))
                .and(activeOnly())
                .and(statusIn(List.of(RideStatus.SCHEDULED, RideStatus.FULL)));

        if(origin != null && !origin.isBlank()) {
            spec = spec.and(origin(origin));
        }
        if(destination != null && !destination.isBlank()) {
            spec = spec.and(destination(destination));
        }

        spec = spec.and(minDepartureTime(minDepartureTime));

        return spec;
    }

    private static Specification<Ride> origin(String origin) {
        return (root, query, cb) -> cb.like(root.get("origin"), "%" + origin + "%");
    }

    private static Specification<Ride> destination(String destination) {
        return (root, query, cb) -> cb.like(root.get("destination"), "%" + destination + "%");
    }

    private static Specification<Ride> includeFull(boolean includeFull) {
        return (root, query, cb) -> includeFull ?
                cb.conjunction() :
                cb.greaterThan(root.get("availableSeats"), 0);
    }

    private static Specification<Ride> minDepartureTime(Instant date) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("departureTime"), date);
    }

    private static Specification<Ride> activeOnly() {
        return (root, query, cb) -> cb.equal(root.get("status"), Status.ACTIVE);
    }

    private static Specification<Ride> statusIn(List<RideStatus> statuses) {
        return (root, query, cb) -> {
            if (statuses == null || statuses.isEmpty()) {
                return cb.conjunction();
            }
            return root.get("rideStatus").in(statuses);
        };
    }
}
