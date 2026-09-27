package com.ifride.core.ride.repository.specs;

import com.ifride.core.ride.model.Ride;
import java.time.Instant;
import org.springframework.data.jpa.domain.Specification;

public class RideSpecs {

//    @Query("SELECT r FROM Ride r WHERE " +
//            "(:origin IS NULL OR r.origin LIKE :origin%) AND " +
//            "(:destination IS NULL OR r.destination LIKE :destination%) AND " +
//            "(:includeFull = true OR r.availableSeats > 0) AND " +
//            "r.departureTime > CURRENT_TIMESTAMP")


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
