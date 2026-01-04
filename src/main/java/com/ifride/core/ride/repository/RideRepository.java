package com.ifride.core.ride.repository;

import com.ifride.core.ride.model.Ride;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RideRepository extends JpaRepository<Ride, String> {
}
