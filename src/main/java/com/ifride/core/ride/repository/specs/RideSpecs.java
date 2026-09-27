package com.ifride.core.ride.repository.specs;


import com.ifride.core.ride.model.Ride;
import java.time.Instant;
import org.springframework.data.jpa.domain.Specification;

public class RideSpecs {

    public static Specification<Ride> buildFindAllSpecs(String origin, String destination, boolean includeFull, Instant minDepartureTime) {
        var spec = Specification.where(includeFull(includeFull));

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
}
