package com.ifride.core.ride.model;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.ride.model.enums.ParticipantStatus;
import com.ifride.core.shared.model.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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

    @Enumerated(EnumType.STRING)
    private ParticipantStatus status = ParticipantStatus.PENDING;

    private LocalDateTime requestedAt = LocalDateTime.now();
}