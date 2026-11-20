package com.ifride.core.driver.repository;

import com.ifride.core.driver.model.entity.Driver;
import com.ifride.core.driver.model.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, String> {
    @Query("SELECT v FROM Vehicle v WHERE v.owner.id = :ownerId")
    List<Vehicle> findByOwnerId(@Param(value = "ownerId") String ownerId);

    List<Vehicle> findByOwner(Driver owner);
    boolean existsByOwnerIdAndPlate(String ownerId, String plate);
}
