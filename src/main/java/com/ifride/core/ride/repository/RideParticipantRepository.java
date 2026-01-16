package com.ifride.core.ride.repository;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.ride.model.RideParticipant;
import com.ifride.core.ride.model.enums.ParticipantStatus;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RideParticipantRepository extends JpaRepository<RideParticipant, String> {


    @Query("""
    SELECT COUNT(rp) > 0
    FROM RideParticipant rp
    WHERE rp.passenger = :passenger
    AND rp.participantStatus = 'ACCEPTED'
    AND rp.ride.rideStatus NOT IN (
        com.ifride.core.ride.model.enums.RideStatus.FINISHED,
        com.ifride.core.ride.model.enums.RideStatus.CANCELLED
    )
    AND rp.ride.departureTime BETWEEN :startTime AND :endTime
""")
    boolean hasConflict(@Param("passenger") User passenger,
                        @Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

    @Modifying
    @Transactional
    @Query("""
        UPDATE RideParticipant rp
        SET rp.participantStatus = com.ifride.core.ride.model.enums.ParticipantStatus.REJECTED
        WHERE rp.passenger.id = :passengerId
        AND rp.ride.id != :acceptedRideId
        AND rp.participantStatus = com.ifride.core.ride.model.enums.ParticipantStatus.PENDING
        AND rp.ride.departureTime BETWEEN :startTime AND :endTime
    """)
    void rejectOverlappingRequests(
            @Param("passengerId") String passengerId,
            @Param("acceptedRideId") String acceptedRideId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    boolean existsByRideIdAndPassengerIdAndParticipantStatusIn(
            String rideId,
            String passengerId,
            Collection<ParticipantStatus> statuses
    );
}
