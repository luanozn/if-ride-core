package com.ifride.core.ride.model;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.mapper.UserMapper;
import com.ifride.core.ride.model.dto.RideParticipantResponseDTO;
import com.ifride.core.ride.model.dto.RideResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class RideMapper {

    private final UserMapper userMapper;

    public RideMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public RideParticipantResponseDTO from(RideParticipant rideParticipant, User requester) {
        return new RideParticipantResponseDTO(
                rideParticipant.getId(),
                RideResponseDTO.fromEntity(rideParticipant.getRide()),
                userMapper.toDto(rideParticipant.getPassenger(), requester),
                rideParticipant.getParticipantStatus(),
                rideParticipant.getRequestedAt()
        );
    }
}
