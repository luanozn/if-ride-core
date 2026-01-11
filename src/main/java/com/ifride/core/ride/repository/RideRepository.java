package com.ifride.core.ride.repository;

import com.ifride.core.ride.model.Ride;
import com.ifride.core.ride.model.enums.RideStatus;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RideRepository extends JpaRepository<Ride, String> {

    @Query("""
        SELECT COUNT(r) > 0
        FROM Ride r
        WHERE r.driver.id = :driverId
        AND r.status = 'ACTIVE'
        AND r.rideStatus IN (
            com.ifride.core.ride.model.enums.RideStatus.SCHEDULED,
            com.ifride.core.ride.model.enums.RideStatus.IN_PROGRESS,
            com.ifride.core.ride.model.enums.RideStatus.FULL
        )
        AND r.departureTime > :startTime
        AND r.departureTime < :endTime
    """)
    boolean existsOverlap(@Param("driverId") String driverId,
                          @Param("startTime") LocalDateTime startTime,
                          @Param("endTime") LocalDateTime endTime);

    @Modifying
    @Query("""
                UPDATE Ride r
                SET r.availableSeats = r.availableSeats - 1
                WHERE r.id = :rideId
                AND r.availableSeats > 0
            """)
    int decrementAvailableSeats(@Param("rideId") String rideId);

    @Modifying
    @Query("UPDATE Ride r SET r.availableSeats = r.availableSeats + 1 WHERE r.id = :rideId AND r.availableSeats < r.totalSeats")
    int incrementAvailableSeats(@Param("rideId") String rideId);

    @Query("SELECT r.availableSeats FROM Ride r WHERE r.id = :id")
    int getCurrentAvailableSeats(@Param("id") String id);

    @Modifying
    @Query("UPDATE Ride r SET r.rideStatus = :status WHERE r.id = :id")
    void updateStatus(@Param("id") String id, @Param("status") RideStatus status);
}
