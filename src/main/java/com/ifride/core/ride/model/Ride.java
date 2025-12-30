package com.ifride.core.ride.model;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.ride.model.enums.RideStatus;
import com.ifride.core.shared.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
public class Ride extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    private String origin;
    private String destination;
    private LocalDateTime departureDate;

    private Integer numberOfSeats;
    private Integer occupiedSeats = 0;

    private RideStatus status = RideStatus.SCHEDULED;

    @ElementCollection
    @CollectionTable(name = "ride_pickup_points", joinColumns = @JoinColumn(name = "ride_id"))
    @Column(name = "point_name")
    private List<String> pickupPoints = new ArrayList<>();

    @Version
    private Long version;

}
