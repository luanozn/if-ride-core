package com.ifride.core.driver.service;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.driver.model.dto.VehicleCreationDTO;
import com.ifride.core.driver.model.dto.VehicleResponseDTO;
import com.ifride.core.driver.model.entity.Vehicle;
import com.ifride.core.driver.repository.VehicleRepository;
import com.ifride.core.shared.exceptions.api.ConflictException;
import com.ifride.core.shared.exceptions.api.ForbiddenException;
import com.ifride.core.shared.exceptions.api.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository repository;
    private final DriverService driverService;

    public VehicleResponseDTO saveBy(VehicleCreationDTO dto, String driverId, User author) {
        var vehicle = new Vehicle();

        if(!author.getId().equals(driverId)) {
            throw new ForbiddenException("Somente o próprio motorista pode criar veículos para si!");
        }

        var owner = driverService.findById(driverId);

        if(repository.existsByOwnerIdAndPlate(driverId, dto.plate())) {
            throw new ConflictException("O veículo com a placa %s já existe pra o usuário %s", dto.plate(), owner.getUser().getEmail());
        }

        vehicle.setModel(dto.model());
        vehicle.setColor(dto.color());
        vehicle.setPlate(dto.plate());
        vehicle.setCapacity(dto.capacity());
        vehicle.setOwner(owner);

        vehicle = repository.save(vehicle);

        return VehicleResponseDTO.fromEntity(vehicle);
    }

    public Vehicle findById(String id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Veículo com o ID %s não encontrado!", id));
    }

    public List<VehicleResponseDTO> getByOwner(String id, User author) {
        if(!author.getId().equals(id) && !author.has(Role.ADMIN)) {
            throw new ForbiddenException("Somente admins e o próprio motorista podem fazer essa solicitação!");
        }

        return repository.findByOwnerId(id)
                .stream()
                .map(VehicleResponseDTO::fromEntity)
                .toList();
    }

    @Transactional
    public void deleteByOwner(String id) {
        List<Vehicle> vehicles = repository.findByOwnerId(id);

        if (vehicles.isEmpty()) {
            return;
        }

        vehicles.forEach(vehicle -> {
            vehicle.setColor("DELETED");
            vehicle.setCapacity(0);
            vehicle.setModel("DELETED");

            vehicle.setPlate("deleted_" + vehicle.getId());
        });

        repository.saveAllAndFlush(vehicles);
        repository.deleteAll(vehicles);
    }

}
