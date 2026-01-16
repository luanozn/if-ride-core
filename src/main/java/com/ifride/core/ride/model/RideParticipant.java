package com.ifride.core.ride.model;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.ride.model.enums.ParticipantStatus;
import com.ifride.core.shared.model.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "ride_participants")
public class RideParticipant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User passenger;

    @Column(name = "pickup_point")
    private String pickupPoint;

    @Enumerated(EnumType.STRING)
    private ParticipantStatus participantStatus = ParticipantStatus.PENDING;


    private LocalDateTime requestedAt = LocalDateTime.now();
}