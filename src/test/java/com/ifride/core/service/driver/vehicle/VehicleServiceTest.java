package com.ifride.core.service.driver.vehicle;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.driver.model.dto.VehicleCreationDTO;
import com.ifride.core.driver.model.entity.Driver;
import com.ifride.core.driver.model.entity.Vehicle;
import com.ifride.core.driver.model.enums.CnhCategory;
import com.ifride.core.driver.repository.VehicleRepository;
import com.ifride.core.driver.service.DriverService;
import com.ifride.core.driver.service.VehicleService;
import com.ifride.core.shared.exceptions.api.ConflictException;
import com.ifride.core.shared.exceptions.api.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository repository;
    @Mock
    private DriverService driverService;

    @InjectMocks
    private VehicleService vehicleService;

    private User author;
    private Driver driver;
    private VehicleCreationDTO dto;

    @BeforeEach
    void setUp() {
        author = new User();
        author.setId("user-1");

        driver = new Driver();
        driver.setId("user-1");
        driver.setUser(author);
        driver.setCnhCategory(CnhCategory.AB);

        dto = new VehicleCreationDTO("Fiat Argo", "ABC1234", "Prata", 4);
    }

    @Test
    @DisplayName("Deve salvar veículo com sucesso")
    void shouldSaveVehicle() {
        given(driverService.findById("user-1")).willReturn(driver);
        given(repository.existsByOwnerIdAndPlate("user-1", "ABC1234")).willReturn(false);
        given(repository.save(any(Vehicle.class))).willAnswer(i -> i.getArgument(0));

        var response = vehicleService.saveBy(dto, "user-1", author);

        assertThat(response.plate()).isEqualTo("ABC1234");
    }

    @Test
    @DisplayName("Deve proibir criar veículo para outro motorista")
    void shouldForbidCreatingVehicleForOthers() {
        assertThatThrownBy(() -> vehicleService.saveBy(dto, "user-2", author))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Somente o próprio motorista");
    }

    @Test
    @DisplayName("Deve proibir placa duplicada para o mesmo motorista")
    void shouldForbidDuplicatePlate() {
        given(driverService.findById("user-1")).willReturn(driver);
        given(repository.existsByOwnerIdAndPlate("user-1", "ABC1234")).willReturn(true);

        assertThatThrownBy(() -> vehicleService.saveBy(dto, "user-1", author))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("já existe");
    }
}