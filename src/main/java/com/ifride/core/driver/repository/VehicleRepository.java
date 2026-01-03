package com.ifride.core.driver.repository;

import com.ifride.core.driver.model.entity.Driver;
import com.ifride.core.driver.model.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, String> {
    List<Vehicle> findByOwner(Driver owner);
    boolean existsByOwnerIdAndPlate(String ownerId, String plate);
}
