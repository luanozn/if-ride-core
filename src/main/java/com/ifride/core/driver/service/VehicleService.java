package com.ifride.core.driver.service;

import com.ifride.core.driver.model.dto.VehicleCreationDTO;
import com.ifride.core.driver.model.dto.VehicleResponseDTO;
import com.ifride.core.driver.model.entity.Vehicle;
import com.ifride.core.driver.repository.VehicleRepository;
import com.ifride.core.shared.exceptions.api.ConflictException;
import com.ifride.core.shared.exceptions.api.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository repository;
    private final DriverService driverService;

    public VehicleResponseDTO saveBy(VehicleCreationDTO dto, String driverId) {
        var vehicle = new Vehicle();

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

    public List<VehicleResponseDTO> getByOwner(String id) {
        var driver = driverService.findById(id);
        return repository.findByOwner(driver)
                .stream()
                .map(VehicleResponseDTO::fromEntity)
                .toList();
    }
}
